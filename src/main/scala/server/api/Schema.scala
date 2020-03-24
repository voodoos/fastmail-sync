package server.api

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import sangria.macros.derive._
import sangria.schema._
import sangria.streaming.{SubscriptionStreamLike, akkaStreams}
import sangria.streaming.akkaStreams._
import model._
import model.traits.Event
import server.Ctx

class Schema(implicit materializer : Materializer) {
  val EventType: InterfaceType[Any, Event] = InterfaceType("Email event", List.empty)

  val EmailCreatedType = deriveObjectType[Unit, EmailCreated](Interfaces(EventType))

  implicit val stream = sangria.streaming.akkaStreams.akkaStreamIsValidSubscriptionStream[Source[Action[Ctx, EmailCreated], NotUsed], Action, Ctx, EmailCreated]

  val SubscriptionType = ObjectType("Subscription", fields[Ctx, Unit](
    Field.subs("emailEvents", EmailCreatedType, resolve = (_ => {
      val e = EmailCreated("testid")
      val t = Source.single(Value[Ctx, EmailCreated](e))
      t
    })
    )
  ))
}
