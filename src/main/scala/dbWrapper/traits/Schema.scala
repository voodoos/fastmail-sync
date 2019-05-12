package dbWrapper.traits

import com.orientechnologies.orient.core.db.ODatabaseSession

import scala.collection.mutable

trait Schema {
  Schemas.register(this)
  def init_schema(implicit db : ODatabaseSession) : Unit
}

object Schemas {
  // Todo: this takes memory at runtime, should be replaced by some reflection
  // or meta programming happening at compile time. Or at least be cleared !
  val all : mutable.Queue[Schema] = mutable.Queue()

  def register(e : Schema) : Unit  = all.enqueue(e); ()

  def write(implicit db : ODatabaseSession) : Unit = {
    all.foreach(_.init_schema)
  }
}