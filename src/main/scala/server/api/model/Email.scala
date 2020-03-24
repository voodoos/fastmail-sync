package server.api.model

import traits.Event

case class Email(message_id: String, subject: String)

object Email {
}


////////////////
//// EVENTS ////
////////////////

sealed abstract case class EmailEvent(id: String) extends Event
case class EmailCreated(override val id: String) extends EmailEvent(id)