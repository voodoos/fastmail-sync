package server.actors

import akka.actor.Actor
import sangria.streaming.akkaStreams._
import server.actors.SubscriptionActor.Subscribe // todo: customize

class SubscriptionManager extends Actor {
  override def receive: Receive = {
    case s : Subscribe => println("Inactor:",s)
    case m => println("Unknown message: ", m)
  }
}
