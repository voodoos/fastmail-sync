package server.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.util.Timeout
import sangria.ast.OperationType
import sangria.execution.{Executor, PreparedQuery}
import sangria.marshalling.sprayJson._
import sangria.parser.QueryParser
import server.generic.Event
import server.{Ctx, schema}
import spray.json._

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object SubscriptionActor extends DefaultJsonProtocol {
  sealed trait SubscriptionMessage
  case class SubscriptionAccepted() extends SubscriptionMessage
  case class QueryResult(json: JsValue) extends SubscriptionMessage

  case class Subscribe(query: String, operation: Option[String])
  case class Connected(outgoing: ActorRef)
  case class PreparedQueryContext(query: PreparedQuery[Ctx, Any, JsObject])

  implicit val subscribeFormat: RootJsonFormat[Subscribe] = jsonFormat2(Subscribe)
}

class SubscriptionActor(publisher: ActorRef, ctx: Ctx)
                       (implicit timeout: Timeout)
  extends Actor with ActorLogging {

  import SubscriptionActor._

  private implicit val ec: ExecutionContextExecutor = context.system.dispatcher

  // The Sangria executor that is initialized with the schema and
  // will be used to execute queries:
  private val sangria_executor = Executor(schema.createSchema)
  private var subscriptions = Map.empty[String, Set[PreparedQueryContext]]

  // Messages handled by this actor:
  override def receive: Receive = {
    case Connected(outgoing) =>
      publisher ! SubscriptionEventPublisher.Join // Register this actor to the publisher
      context.become(connected(outgoing))
  }

  /*
  Akka supports hotswapping the Actor’s message loop (e.g. its implementation) at runtime:
  invoke the context.become method from within the Actor. become takes a PartialFunction[Any, Unit]
  that implements the new message handler.
  The hotswapped code is kept in a Stack which can be pushed and popped.
   */

  private def connected(outgoing: ActorRef): Receive = {
    case e: Subscribe =>
      log.info(s"Got sub: $e")
      prepareQuery(e)

    case context: PreparedQueryContext =>
      log.info(s"Query is prepared: $context")
      outgoing ! SubscriptionAccepted()
      context.query.fields.map(_.field.name).foreach { field =>
        subscriptions = subscriptions.updated(field, subscriptions.get(field) match {
          case Some(contexts) => contexts + context
          case _ => Set(context)
        })
      }

    case event: Event =>
      val fieldName = schema.subscriptionFieldName(event)
      queryContextsFor(fieldName) foreach { ctx =>
        ctx.query.execute(root = event) map { result =>
          outgoing ! QueryResult(result)
        }
      }
  }

  private def queryContextsFor(fieldName: Option[String]) = fieldName match {
    case Some(name) => subscriptions.getOrElse(name, Set.empty[PreparedQueryContext])
    case _ => Set.empty[PreparedQueryContext]
  }

  private def prepareQuery(subscription: Subscribe) = {
    QueryParser.parse(subscription.query) match {
      case Success(ast) =>
        ast.operationType(subscription.operation) match {
          case Some(OperationType.Subscription) =>
            sangria_executor.prepare(ast, ctx, (), subscription.operation, JsObject.empty).map {
              query => self ! PreparedQueryContext(query)
            }
          case x =>
            log.warning(s"OperationType: $x not supported with WebSockets. Use HTTP POST")
        }

      case Failure(e) =>
        log.warning(e.getMessage)
    }
  }

}
