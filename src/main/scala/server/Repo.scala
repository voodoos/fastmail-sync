package server

import akka.actor.ActorRef
import akka.util.Timeout

import scala.concurrent.ExecutionContext

case class Repo(messages: ActorRef,
                eventStore: ActorRef,
                ec: ExecutionContext,
                to: Timeout) {
  implicit def executionContext = ec
  implicit def timeout = to
}