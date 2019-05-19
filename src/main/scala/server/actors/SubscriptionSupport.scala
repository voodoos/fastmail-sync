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

trait SubscriptionSupport {

  import SubscriptionActor._

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
