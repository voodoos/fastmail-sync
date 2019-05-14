package server

import generic.View

class MessageView extends View[Message, MessageEvent] {
  val handleEvent: Handler = {
    case event: MessageCreated ⇒
      add(Message(event.id, event.subject))
    case event: MessageDeleted ⇒
      delete(event)
  }
}
