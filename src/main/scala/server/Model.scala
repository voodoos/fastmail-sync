package server

import generic.{Event, Versioned}

case class Message(id: String, subject: String)

sealed trait MessageEvent extends Event

case class MessageCreated(id: String, subject: String) extends MessageEvent

case class MessageDeleted(id: String) extends MessageEvent