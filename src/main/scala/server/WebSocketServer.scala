package server

import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import akka.stream.actor.{ActorPublisher, ActorSubscriber}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import generic.{Event, MemoryEventStore}
import sangria.ast.OperationType
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.sprayJson._
import sangria.parser.{QueryParser, SyntaxError}
import server.actors.{SubscriptionEventPublisher, SubscriptionSupport}
import spray.json._

import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps
import scala.util.{Failure, Success}

object WebSocketServer extends SubscriptionSupport {
  implicit val system: ActorSystem = ActorSystem("server")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val logger = Logging(system, getClass)

  import system.dispatcher

  implicit val timeout: Timeout = Timeout(10 seconds)

  val articlesView = system.actorOf(Props[ArticleView])
  val articlesSink = Sink.fromSubscriber(ActorSubscriber[ArticleEvent](articlesView))

  val authorsView = system.actorOf(Props[AuthorView])
  val authorsSink = Sink.fromSubscriber(ActorSubscriber[AuthorEvent](authorsView))

  val eventStore = system.actorOf(Props[MemoryEventStore])
  val eventStorePublisher =
    Source.fromPublisher(ActorPublisher[Event](eventStore))
      .runWith(Sink.asPublisher(fanout = true))

  val subscriptionEventPublisher = system actorOf Props(new SubscriptionEventPublisher(eventStorePublisher))

  // Connect event store to views
  Source.fromPublisher(eventStorePublisher).collect {
    case event: ArticleEvent ⇒ event
  }.to(articlesSink).run()
  Source.fromPublisher(eventStorePublisher).collect {
    case event: AuthorEvent ⇒ event
  }.to(authorsSink).run()

  // The ExecutionContext
  private val ctx = Ctx(authorsView, articlesView, eventStore, system.dispatcher, timeout)

  // The Sangria executor
  private val executor = Executor(schema.createSchema)

  private def executeQuery(query: String, operation: Option[String], variables: JsObject = JsObject.empty) =
    QueryParser.parse(query) match {
      case Success(queryAst) =>
        queryAst.operationType(operation) match {

          case Some(OperationType.Subscription) ⇒
            complete(ToResponseMarshallable(BadRequest → JsString("Subscriptions not supported via HTTP. Use WebSockets")))

          // all other queries will just return normal JSON response
          case _ =>
            complete(
              executor.execute(queryAst, ctx, (), operation, variables
              ).map(OK -> _)
              .recover {
                case error: QueryAnalysisError =>
                  BadRequest -> error.resolveError
                case error: ErrorWithResolver =>
                  InternalServerError -> error.resolveError
              })
        }

      case Failure(error: SyntaxError) =>
        complete(ToResponseMarshallable(BadRequest → JsObject(
          "syntaxError" -> JsString(error.getMessage),
          "locations" -> JsArray(JsObject(
            "line" -> JsNumber(error.originalError.position.line),
            "column" -> JsNumber(error.originalError.position.column)
          ))
        )))

      case Failure(error) =>
        complete(
          ToResponseMarshallable(InternalServerError ->
            JsString(error.getMessage)
          )
        )
    }

  val route: Route = cors() { //todo : finer cors grain than default
    path("graphql") {
      post {
        // Handle standard post request
        entity(as[JsValue]) { requestJson =>
          // Get all request fields (using pattern matching)
          val JsObject(fields) = requestJson

          // Get content of the query field
          val JsString(query) = fields("query")

          // Get the operation from the query (should be a string)
          val operation = fields.get("operationName") collect {
            case JsString(op) => op
          }

          // Get variables
          val vars = fields.get("variables") match {
            case Some(obj: JsObject) => obj
            case _ => JsObject.empty
          }

          executeQuery(query, operation, vars)
        }
      } ~
        // Handle websocket upgrade requests
      get(handleWebSocketMessages(graphqlSubscriptionHandler))
        //get(handleWebSocketMessages(graphQlSubscriptionSocketHandler(subscriptionEventPublisher, ctx)))
    } ~
      (get & path("client")) {
        getFromResource("web/client.html")
      } ~
      get {
        getFromResource("web/graphiql.html")
      }
  }

  def run() =  {
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

    StdIn.readLine()

    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
