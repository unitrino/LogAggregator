package lightit.DBWork

import java.sql.Timestamp
import lightit.JsonConverts.toJsonConvert._
import org.apache.commons.dbcp2.BasicDataSource
import spray.json.JsArray

import scala.collection.mutable.ArrayBuffer

//Класс доступа к БД

class WorkWithDB {
  val dbUrl = "jdbc:postgresql://localhost/lightit?user=lit&password=12345"
  val connectionPool = new BasicDataSource()

  connectionPool.setDriverClassName("org.postgresql.Driver")
  connectionPool.setUrl(dbUrl)
  connectionPool.setInitialSize(10)

  val stmt = connectionPool.getConnection.createStatement()

  //Инициализация таблиц
  stmt.executeUpdate("DROP TYPE IF EXISTS level")
  stmt.executeUpdate("CREATE TYPE level AS ENUM ('info', 'debug', 'warning', 'error','critical')")
  stmt.executeUpdate("CREATE TABLE channels(id serial PRIMARY KEY, URL text, err serial,critical serial, total_logs serial)")
  stmt.executeUpdate("CREATE TABLE messages(id serial PRIMARY KEY, chan_id INTEGER NOT NULL REFERENCES channels ON DELETE CASCADE ON UPDATE CASCADE, mes_level level,source_ip CIDR, title TEXT,data TEXT, tags TEXT[], ts TIMESTAMP)")
  stmt.executeUpdate("CREATE TABLE chains(id serial PRIMARY KEY, channel_id INTEGER NOT NULL REFERENCES channels ON DELETE CASCADE ON UPDATE CASCADE, title text,mes_level level,last_appears TIMESTAMP,num_messages INTEGER, tags TEXT[],json_data TEXT)")
  stmt.executeUpdate("CREATE TABLE chains_to_messages(chains_id INTEGER NOT NULL REFERENCES chains ON DELETE CASCADE ON UPDATE CASCADE,mess_id INTEGER NOT NULL REFERENCES messages ON DELETE CASCADE ON UPDATE CASCADE)")

  //Обноление списка каналов
  def updateChannelList(idChannel:Int)  {
    val rs_err = stmt.executeQuery(s"SELECT sum(num_messages) FROM chains WHERE channel_id = '$idChannel' AND mes_level = 'error' ")
    //Кол-во сообщений с ошибками
    val numbErrors = {
      if(rs_err.next()) {
        rs_err.getInt(1)
      }
      else 0
    }
    rs_err.close()
    //Кол-во критических сообщений
    val rs_crit = stmt.executeQuery(s"SELECT sum(num_messages) FROM chains WHERE channel_id = '$idChannel' AND mes_level = 'critical' ")
    val numbCritical = {
      if(rs_crit.next()) {
        rs_crit.getInt(1)
      }
      else 0
    }
    rs_crit.close()
    //Суммарное кол-во сообщений
    val rs_summ = stmt.executeQuery(s"SELECT sum(num_messages) FROM chains WHERE channel_id = '$idChannel'")
    val numbSummary = {
      if(rs_summ.next()) {
        rs_summ.getInt(1)
      }
      else 0
    }
    rs_summ.close()
    stmt.executeUpdate(s"UPDATE channels SET err = '$numbErrors',critical = '$numbCritical',total_logs = '$numbSummary' WHERE id = '$idChannel'")
  }

  //Создание/добавление нового канала
  def createNewChannel =  {
    val dbId = stmt.executeQuery("select last_value from channels_id_seq")
    dbId.next()
    val val1 = dbId.getInt(1)
    dbId.close()

    val dbId2 = stmt.executeQuery("select max(id) from channels")
    dbId2.next()
    val val2 = dbId2.getInt(1)
    dbId2.close()

    val idChannel = {
      if(val2 == 1)
      {
        val count_elems = val2 + 1
        count_elems
      }
      else if(val1 > 1)
      {
        val count_elems = val1 + 1
        count_elems
      }
      else 1
    }
    val url = "http//localhost:8080/channels/" + idChannel.toString
    stmt.executeUpdate(s"insert into channels(URL,err,critical,total_logs) values ('$url',0,0,0)")
    updateChannelList(idChannel)
  }

  //Удаление канала
  def deleteChannel(idChannel:Int) = {
    stmt.executeUpdate(s"delete from channels where id = $idChannel")
  }

  //Добавление нового сообщения
  def addNewMessage(channelId:Int,messLevel:String,srcIp:String,title:String,jsonData:String,tags:Array[String]) {
    val rs = stmt.executeQuery(s"insert into messages(chan_id,mes_level,source_ip,title,data,tags,ts) values ('$channelId','$messLevel','$srcIp','$title','$jsonData','{${tags.mkString(",")}}', now()) RETURNING ts,id")
    rs.next()
    val lastTimestamp = rs.getTimestamp(1)
    val messId = rs.getInt(2)
    addToMessageChains(messId,channelId,title,messLevel,lastTimestamp,tags,jsonData)
  }

  //Получение id всех сообщений
  def getAllChannelsId() = {
    val rs = stmt.executeQuery("SELECT id FROM channels")
    var list_channel = ArrayBuffer[Int]()
    while (rs.next()) {
      val chanId = rs.getInt(1)
      list_channel += chanId
    }
    list_channel
  }

  //Получение списка всех каналов и обращение его в json
  def getAllChannels() = {
    val rs = stmt.executeQuery("SELECT * FROM channels")
    val listOfChannels = ArrayBuffer[Channel]()//SortedMap[Int,String]()
    while (rs.next()) {
      val currValue = Channel(rs.getInt(1),rs.getString(2),rs.getInt(3),rs.getInt(4),rs.getInt(5))
      listOfChannels += currValue
    }
    JsArray(listOfChannels.map(elem => elem.toJson).toVector)
  }

  //Проверка существования канала с заданным id
  def ifChannelExists(channelId:Int):Int = {
    val rs = stmt.executeQuery(s"SELECT count(id) FROM channels where id = $channelId")
    rs.next()
    rs.getInt(1)
  }

  //Сортировка цепочек по дате их последнего обновления
  def sortChainsByData(channelId:Int) = {
    val rs = stmt.executeQuery(s"SELECT id, title,mes_level,last_appears,num_messages,tags FROM chains WHERE channel_id = $channelId ORDER BY last_appears")
    var listOfChains = ArrayBuffer[MessageChain]()
    while (rs.next()) {
      val currMesageChain = MessageChain(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getInt(5),rs.getString(6))
      listOfChains += currMesageChain
    }
    JsArray(listOfChains.map(elem => elem.toJson).toVector)
  }

  //Сортировка цепочек по кол-ву сообщений
  def sortChainsByMessNumbers(channelId:Int) = {
    val rs = stmt.executeQuery(s"SELECT id, title,mes_level,last_appears,num_messages,tags FROM chains WHERE channel_id = '$channelId' ORDER BY num_messages")
    var listOfChains = ArrayBuffer[MessageChain]()
    while (rs.next()) {
      val currMesageChain = MessageChain(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getInt(5),rs.getString(6))
      listOfChains += currMesageChain
    }
    JsArray(listOfChains.map(elem => elem.toJson).toVector)
  }


  //Фильтрация цепочек по уровню критичности
  def filterChainsByLevel(channelId:Int,messageLevel:String) = {
    val rs = stmt.executeQuery(s"SELECT id, title,mes_level,last_appears,num_messages,tags FROM chains WHERE channel_id = $channelId AND mes_level = '$messageLevel'")
    var listOfChains = ArrayBuffer[MessageChain]()
    while (rs.next()) {
      val currMesageChain = MessageChain(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getInt(5),rs.getString(6))
      listOfChains += currMesageChain
    }
    JsArray(listOfChains.map(elem => elem.toJson).toVector)
  }

  //Фильтрация цепочек по тегам
  def filterChainsByTags(channelId:Int,tags:Array[String]) = {
    val rs = stmt.executeQuery(s"SELECT id, title,mes_level,last_appears,num_messages,tags FROM chains WHERE channel_id = $channelId AND tags = '{${tags.mkString(",")}}'")
    var listOfChains = ArrayBuffer[MessageChain]()
    while (rs.next()) {
      val currMesageChain = MessageChain(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getInt(5),rs.getString(6))
      listOfChains += currMesageChain
    }
    JsArray(listOfChains.map(elem => elem.toJson).toVector)
  }

  //Получение уровней критичности и колличества сообщений, его содержащих
  def getAllLevelsAndNumberFromChains(channelId:Int) = {
    val rs = stmt.executeQuery(s"SELECT SUM(num_messages),mes_level from chains WHERE channel_id = '$channelId' GROUP BY mes_level")
    val result = ArrayBuffer[LevelsAndNumbers]()
    while (rs.next()) {
      val currValue = LevelsAndNumbers(rs.getString(2),rs.getInt(1))
      result += currValue
    }
    JsArray(result.map(elem => elem.toJson).toVector)
  }

  //Получение тегов и колличества сообщений, его содержащих
  def getAllTagsAndNumberFromChains(channelId:Int) = {
    val rs = stmt.executeQuery(s"SELECT SUM(num_messages),tags FROM chains WHERE channel_id = '$channelId' GROUP BY tags")
    val result = ArrayBuffer[TagsAndNumbers]()
    while (rs.next()) {
      val currValue = TagsAndNumbers(rs.getString(2),rs.getInt(1))
      result += currValue
    }
    JsArray(result.map(elem => elem.toJson).toVector)
  }

  //Добавление сообщения в цепочку
  def addToMessageChains(messId:Int,channelId:Int,title:String,level:String,ts:Timestamp,tags:Array[String],jsonData:String) = {
    val rs = stmt.executeQuery(s"SELECT count(id) FROM chains LIMIT 1")
    var chainId = 0
    rs.next()
    if(rs.getInt(1) > 0) {
      val rs2 = stmt.executeQuery(s"SELECT num_messages,id FROM chains WHERE channel_id = '$channelId' AND title = '$title' AND mes_level = '$level' AND tags = '{${tags.mkString(",")}}' AND json_data = '$jsonData'")
      if(rs2.next()) {
        chainId = rs2.getInt(2)
        var count = rs2.getInt(1)
        count += 1
        stmt.executeUpdate(s"UPDATE chains SET num_messages = '$count', last_appears = '$ts' WHERE id = '$chainId'")
      }
      else {
        val rs3 = stmt.executeQuery(s"INSERT INTO chains(channel_id,title,mes_level,last_appears,num_messages,tags,json_data) values ($channelId,'$title','$level','$ts',1,'{${tags.mkString(",")}}','$jsonData') RETURNING id")
        rs3.next()
        chainId = rs3.getInt(1)
      }
    }
    else {
      val rs3 = stmt.executeQuery(s"INSERT INTO chains(channel_id,title,mes_level,last_appears,num_messages,tags,json_data) values ('$channelId','$title','$level','$ts','1','{${tags.mkString(",")}}','$jsonData') RETURNING id")
      rs3.next()
      chainId = rs3.getInt(1)
    }
    stmt.executeUpdate(s"INSERT INTO chains_to_messages VALUES ('$chainId','$messId')")
  }

  //Получение всех цепочек привязанных к каналу и возврат их в формате json
  def getMessageChain(channelId:Int) = {
    val rs = stmt.executeQuery(s"SELECT id, title,mes_level,last_appears,num_messages,tags FROM chains WHERE channel_id = '$channelId'")
    var listOfChains = ArrayBuffer[MessageChain]()//Map[Int,String]()
    while (rs.next()) {
      val currMesageChain = MessageChain(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getInt(5),rs.getString(6))
      listOfChains += currMesageChain
    }
    JsArray(listOfChains.map(elem => elem.toJson).toVector)
  }

  //Извлечение всех сообщений выбранной цепочки и возврат их в формате json
  def getMessagesFromChain(chainId:Int) = {
    val rs = stmt.executeQuery(s"SELECT id,mes_level,source_ip,title,data,tags,ts FROM messages WHERE id IN (SELECT mess_id FROM chains_to_messages WHERE chains_id = '$chainId')")
    val listOfMessages = ArrayBuffer[Message]()
    while (rs.next()) {
      val currMess = Message(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7))
      listOfMessages += currMess
    }
    JsArray(listOfMessages.map(elem => elem.toJson).toVector)
  }

  //Очистка БД
  def clear_db = {
    stmt.executeUpdate("DROP TABLE messages CASCADE")
    stmt.executeUpdate("DROP TABLE channels CASCADE")
    stmt.executeUpdate("DROP TABLE chains_to_messages CASCADE")
    stmt.executeUpdate("DROP TYPE level CASCADE")
    stmt.executeUpdate("DROP TABLE chains CASCADE")
  }
}

