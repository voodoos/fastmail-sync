package server.actors

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.Timeout
import server.Ctx
import spray.json._

import scala.util.{Success, Try}

// This trait is implemented by WebSocketServer
trait SubscriptionSupport {

  import SubscriptionActor._

  // experimentation
  protected def graphqlSubscriptionHandler
  (implicit system: ActorSystem) = {
    //Prepare actor to handle messages
    val actor = system.actorOf(Props(new SubscriptionManager))

    val incoming = Flow[Message]
      .alsoTo(Sink.foreach(println(_)))
      .collect {
        // Discard byte messages and parse todo: more general messages for other
        // purposes like fetching body of mail or attachments (but maybe get or post
        // and not websocket are better suited for this.)
        case TextMessage.Strict(input) => Try(input.parseJson.convertTo[Subscribe])
      }
      .collect { case Success(value) => value }
      .alsoTo(Sink.foreach(println(_)))
      // Send the message to an actor
      .to(Sink.actorRef(actor, PoisonPill))

    Flow.fromSinkAndSource(incoming, Source.empty)
  }

  protected def graphQlSubscriptionSocketHandler
  (publisher: ActorRef, ctx: Ctx)
  (implicit system: ActorSystem, materializer: ActorMaterializer, timeout: Timeout): Flow[Message, TextMessage.Strict, NotUsed] = {

    // One actor per subscription
    val subscriptionActor = system.actorOf(Props(new SubscriptionActor(publisher, ctx)))

    // Transform any incoming messages into Subscribe messages and let the subscription actor know about it
    val incoming = Flow[akka.http.scaladsl.model.ws.Message]
      .collect { case TextMessage.Strict(input) => Try(input.parseJson.convertTo[Subscribe]) }
      .collect { case Success(subscription) => subscription }
      .to(Sink.actorRef[Subscribe](subscriptionActor, PoisonPill)) // Give messages to the actor and poison him when finished

    // connect the subscription actor with the outgoing WebSocket actor and transform a result into a WebSocket message.
    val outgoing = Source.actorRef[QueryResult](10, OverflowStrategy.fail)
      .mapMaterializedValue { outputActor =>
        subscriptionActor ! Connected(outputActor)
        NotUsed
      }
      .map { msg: SubscriptionMessage =>
        msg match {
          case result: QueryResult => TextMessage(result.json.compactPrint)
          case result: SubscriptionAccepted =>
            TextMessage("subscription accepted.".toJson.compactPrint)
        }
      }

    // Return the flow describing how ws messages should be handled:
    Flow.fromSinkAndSource(incoming, outgoing)

    /*
    *     +----------------------------------------------+
    *     | Resulting Flow[I, O, NotUsed]                |
    *     |                                              |
    *     |  +---------+                  +-----------+  |
    *     |  |         |                  |           |  |
    * I  ~~> | Sink[I] | [no-connection!] | Source[O] | ~~> O
    *     |  |         |                  |           |  |
    *     |  +---------+                  +-----------+  |
    *     +----------------------------------------------+
    */
  }

}
