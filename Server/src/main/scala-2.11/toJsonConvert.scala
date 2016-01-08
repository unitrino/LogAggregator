package lightit.JsonConverts
import spray.json.DefaultJsonProtocol

//Синглтон для преобразования case классов в json представление
object toJsonConvert extends DefaultJsonProtocol {

  //Case классы реализующие абстракции системы
  case class MessageChain(chainId: Int, title:String, mes_level:String, last_appears:String, num_messages:Int ,tags:String)
  case class Message(messId: Int, level:String, source_ip:String, title:String, data:String ,tags:String, timestamp:String)
  case class Channel(channelId: Int, URL:String, err:Int, criticals:Int, total_logs:Int)
  case class LevelsAndNumbers(level:String,numberOfMessages:Int)
  case class TagsAndNumbers(tags:String,numberOfMessages:Int)

  //Неявные преобразование в json
  implicit val messageChainJsonFormat = jsonFormat6(MessageChain)
  implicit val messageJsonFormat = jsonFormat7(Message)
  implicit val channelChainJsonFormat = jsonFormat5(Channel)
  implicit val levelsAndNumberJsonFormat = jsonFormat2(LevelsAndNumbers)
  implicit val tagsAndNumberJsonFormat = jsonFormat2(TagsAndNumbers)


}