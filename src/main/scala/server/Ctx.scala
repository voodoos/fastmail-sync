package server

import akka.util.Timeout
import schema.MutationError
import akka.actor.ActorRef
import generic.Event
import generic.MemoryEventStore._
import generic.View.Get

import akka.pattern.ask

import scala.concurrent.{ExecutionContext, Future}

case class Ctx(authors: ActorRef, articles: ActorRef, eventStore: ActorRef, ec: ExecutionContext, to: Timeout) extends Mutation {
  implicit def executionContext = ec
  implicit def timeout = to

  def addEvent[T](view: ActorRef, event: Event) =
    (eventStore ? AddEvent(event)).flatMap {
      case EventAdded(_) ⇒
        (view ? Get(event.id, Some(event.version))).mapTo[Option[T]]
      case OverCapacity(_) ⇒
        throw MutationError("Service is overloaded.")
      case ConcurrentModification(_, latestVersion) ⇒
        throw MutationError(s"Concurrent Modification error for entity '${event.id}'. Latest entity version is '$latestVersion'.")
    }

  def addDeleteEvent(event: Event) =
    (eventStore ? AddEvent(event)).map {
      case EventAdded(e) ⇒  e
      case OverCapacity(_) ⇒
        throw MutationError("Service is overloaded.")
      case ConcurrentModification(_, latestVersion) ⇒
        throw MutationError(s"Concurrent Modification error for entity '${event.id}'. Latest entity version is '$latestVersion'.")
    }

  def loadLatestVersion(id: String, version: Long): Future[Long] =
    (eventStore ? LatestEventVersion(id)) map {
      case Some(latestVersion: Long) if version != latestVersion ⇒
        throw MutationError(s"Concurrent Modification error for entity '$id'. Latest entity version is '$latestVersion'.")
      case Some(version: Long) ⇒
        version + 1
      case _ ⇒
        throw MutationError(s"Entity with ID '$id' does not exist.")
    }
}
