package server.api

import server.api.model._
import spray.json.{DefaultJsonProtocol, JsonFormat, RootJsonFormat}

case class WSMessage[T](kind: String, payload: T)

object JsonProtocol extends DefaultJsonProtocol {
  implicit def ws_messageFormat[T : JsonFormat]: RootJsonFormat[WSMessage[T]]
  = jsonFormat2(WSMessage.apply[T])
  implicit val messageFormat: RootJsonFormat[Message] = jsonFormat2(Message.apply)
}
