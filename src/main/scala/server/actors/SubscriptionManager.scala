package server.actors

import akka.actor.Actor
import server.actors.SubscriptionActor.Subscribe

class SubscriptionManager extends Actor {
  override def receive: Receive = {
    case s : Subscribe => println("Inactor:",s)
    case m => println("Unknown message: ", m)
  }
}
