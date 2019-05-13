package server

import spray.json._
import spray.json.DefaultJsonProtocol.jsonFormat2

final case class WSMessage (token: String)//, payload: JsValue)

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val wSFormat: RootJsonFormat[WSMessage] = jsonFormat1(WSMessage)
}
