package server.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import org.reactivestreams.Publisher
import server.generic.Event

object SubscriptionEventPublisher {
  case object Join
}

class SubscriptionEventPublisher(publisher: Publisher[Event]) extends Actor with ActorLogging {

  import SubscriptionEventPublisher._

  implicit val materializer = ActorMaterializer()

  // List of joined actors
  var subscribers: Set[ActorRef] = Set.empty

  Source.fromPublisher(publisher)
    .buffer(100, OverflowStrategy.fail)
    .to(Sink.foreach(e => subscribers.foreach(_ ! e)))
    .run()

  def receive: Receive = {
    case Join =>
      log.info(s"${sender()} joined.")
      subscribers += sender()
      context.watch(sender()) // Deathwatch. Wait for Terminated messages

    case Terminated(subscriber) =>
      log.info(s"${sender()} was terminated.")
      subscribers -= subscriber
  }
}
