package server

import akka.actor.ActorRef
import akka.util.Timeout
import sangria.execution.UserFacingError
import sangria.schema._
import sangria.macros.derive._
import akka.pattern.ask
import server.generic.View
import shapeless.ops.zipper.Get

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

object Schema {
  case class MutationError(mess : String) extends Exception(mess) with UserFacingError

  def createSchema(implicit timeout: Timeout, ec: ExecutionContext) = {

    // Derive schema from model
    implicit val MessageType = deriveObjectType[Repo, Message]()

    val IdArg = Argument("id", StringType)
    val OffsetArg = Argument("offset", OptionInputType(IntType), 0)
    val LimitArg = Argument("limit", OptionInputType(IntType), 100)

    def entityFields[T](name: String,
                        tpe: ObjectType[Repo, T],
                        actor: Repo => ActorRef) =
      fields[Repo, Any](
      Field(name, OptionType(tpe),
        arguments = IdArg :: Nil,
        resolve = c => (actor(c.ctx) ? Get(c.arg(IdArg.name))).mapTo[Option[T]]),
      Field(name + "s", ListType(tpe),
        arguments = OffsetArg :: LimitArg :: Nil,
        resolve = c => (actor(c.ctx) ? View.List(c.arg(OffsetArg), c.arg(LimitArg))).mapTo[Seq[T]]))

    val Query = ObjectType(
      "Query",
      entityFields[Message]("message", MessageType, _.messages)
      )
  }
}

/* From docs:
The resolve argument of a Field expects a function of type Context[Ctx, Val] ⇒ Action[Ctx, Res]. As you can see, the result of the resolve is an Action type which can take different shapes. Here is the list of supported actions:

    Value - a simple value result. If you want to indicate an error, you need to throw an exception
    TryValue - a scala.util.Try result
    FutureValue - a Future result
    PartialValue - a partially successful result with a list of errors
    PartialFutureValue - a Future of partially successful result
    DeferredValue - used to return a Deferred result (see the Deferred Values and Resolver section for more details)
    DeferredFutureValue - the same as DeferredValue but allows you to return Deferred inside of a Future
    UpdateCtx - allows you to transform a Ctx object. The transformed context object would be available for nested sub-objects and subsequent sibling fields in case of mutation (since execution of mutation queries is strictly sequential). You can find an example of its usage in the Authentication and Authorisation section.

Normally the library is able to automatically infer the Action type, so that you don’t need to specify it explicitly.


Many schema elements, like ObjectType, Field or Schema itself, take two type parameters: Ctx and Val:

    Val - represent values that are returned by the resolve function and given to the resolve function as a part of the Context. In the schema example, Val can be a Human, Droid, String, etc.
    Ctx - represents some contextual object that flows across the whole execution (and doesn’t change in most of the cases). It can be provided to execution by the user in order to help fulfill the GraphQL query. A typical example of such a context object is a service or repository object that is able to access a database. In the example schema, some of the fields (like droid or human) make use of it in order to access the character repository.


 */
