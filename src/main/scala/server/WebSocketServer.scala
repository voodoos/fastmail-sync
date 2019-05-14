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
import sangria.ast.OperationType
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.parser.{QueryParser, SyntaxError}
import spray.json._

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import scala.util.{Failure, Success}

object WebSocketServer {
  implicit val system : ActorSystem = ActorSystem("my-system")
  implicit val materializer : ActorMaterializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext : ExecutionContextExecutor = system.dispatcher

  val executor = Executor(Schema.createSchema)



  def executeQuery(query: String, operation: Option[String], variables: JsObject = JsObject.empty) = {
    QueryParser.parse(query) match {
      case Success(queryAst) =>
        queryAst.operationType(operation) match {
            // Subbscription queries should be handled by WS
          case Some(OperationType.Subscription) =>
            complete(ToResponseMarshallable(
              BadRequest -> JsString("Subscriptions not supported via HTTP. Use WebSockets")
            ))
          // all other queries will just return normal JSON response
          case _ ⇒
            complete(executor.execute(queryAst, ctx, (), operation, variables)
              .map(OK → _)
              .recover {
                case error: QueryAnalysisError ⇒ BadRequest → error.resolveError
                case error: ErrorWithResolver ⇒ InternalServerError → error.resolveError
              })
        }
    }
  }


  def run(): Unit = {


    val route =
      path("ws") {
        handleWebSocketMessages(greeter)
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

    StdIn.readLine()

    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
