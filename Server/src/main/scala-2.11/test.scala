package lightit.MainWork

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable.apply
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer}

import lightit.DBWork._


object Main extends App {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  //Объект доступа к БД
  val db_source = new WorkWithDB()
  //Префиксы путей доступа
  val route =
    path ("")
    {
      get {
        //Стартовая страница
        complete("Start")
      }
    }~
    path("chains_detail") {
      get {
        entity(as[FormData]) {
          idd =>
            //Возврат сообщений связанных с цепочкой
            val respMap = idd.fields.toMap
            val chainId = respMap.get("details_chain").get.toInt
            val resp = db_source.getMessagesFromChain(chainId)
            complete(resp.toString())
        }
      }
    } ~
    path("get_chains") {
          get {
            entity(as[FormData]) {
              idd =>
                //Получение цепочек сообщений
                val respMap = idd.fields.toMap
                val channel_id = respMap.get("id_channel").get.toInt
                val resp = {
                    if(respMap.getOrElse("by_data","NONE") != "NONE") {
                      db_source.sortChainsByData(channel_id)
                    }
                    else if(respMap.getOrElse("by_number","NONE") != "NONE") {
                      db_source.sortChainsByMessNumbers(channel_id)
                    }
                    else if (respMap.getOrElse("levels", "NONE") != "NONE") {
                      val respLevel = respMap.get("levels").get.toString
                      db_source.filterChainsByLevel(channel_id, respLevel)
                    }
                    else if (respMap.getOrElse("tags", "NONE") != "NONE") {
                      val respTags = respMap.get("tags").toArray
                      db_source.filterChainsByTags(channel_id, respTags)
                    }
                    else {
                      db_source.getMessageChain(channel_id)
                    }
                }
                val tagsAndNumb = db_source.getAllTagsAndNumberFromChains(channel_id)
                val levelsAndNumb = db_source.getAllLevelsAndNumberFromChains(channel_id)
                import spray.json.JsArray
                complete(JsArray(resp,tagsAndNumb,levelsAndNumb).toString)
            }
          }
    } ~
    path("channels" / IntNumber) {
        channel_id:Int => {
          put {
            entity(as[FormData]) {
              formData => {
                //Прием каналом входищих сообщений логов
                if (db_source.ifChannelExists(channel_id) > 0) {
                  val mapResp = formData.fields.toMap
                  val dbConn = db_source
                  val messLevel = mapResp.get("level").get
                  val srcIp = mapResp.get("source_ip").get
                  val title = mapResp.get("title").get
                  val jsonData = mapResp.get("data").get
                  val tagsArray = mapResp.get("tags").get.split(',')

                  dbConn.addNewMessage(channel_id, messLevel, srcIp, title, jsonData, tagsArray)
                  complete("data for " + channel_id.toString + " is ready")
                }
                else complete("channel does not exists")
              }
            }
          }
        }
      } ~
      path("tables") {
        post {
          entity(as[FormData]) {
            formData =>
                {
                  //Зарегистрированные каналы
                  val map_resp = formData.fields.toMap
                  val list_chanell = db_source.getAllChannels()
                  if (map_resp.getOrElse("delete_channel","NONE") != "NONE")
                  {
                    val deleteIdChannel:Int = map_resp.get("id_channel").get.toInt
                    db_source.deleteChannel(deleteIdChannel)
                    val updatedChannel = db_source.getAllChannels()
                    complete(updatedChannel.toString())
                  }
                  else if (map_resp.getOrElse("new_channel","NONE") == "NONE" || map_resp == Map()) {
                    db_source.getAllChannelsId().foreach { id => db_source.updateChannelList(id)}
                    val listChannels = db_source.getAllChannels()
                    complete(listChannels.toString())
                  }
                  else {
                    db_source.createNewChannel
                    db_source.getAllChannelsId().foreach { id => db_source.updateChannelList(id)}
                    val listChannels = db_source.getAllChannels()
                    complete(listChannels.toString())
                  }
                }
          }
        } ~
        get {
          //Обновление списка каналов
          db_source.getAllChannelsId().foreach { id  => db_source.updateChannelList(id)}
          val listChannels = db_source.getAllChannels()
          complete(listChannels.toString())
        }
      }

  //Формирование akka stream на роуты и привязка к ip, порту
  val bindingFuture = Http().bindAndHandle(route, "localhost",8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  import scala.io._
  StdIn.readLine()
  db_source.clear_db
  bindingFuture.flatMap(_.unbind()).onComplete(_ => system.shutdown())
}
