package dbWrapper

import com.orientechnologies.orient.core.db.ODatabaseSession
import com.orientechnologies.orient.core.record.{OElement, OVertex}

abstract class Vertex(protected val vertex : OVertex) extends Element(vertex) {
  def getVertex : OVertex = vertex
}

object Vertex {
  def create(db : ODatabaseSession, _class : Class, props : Map[String, Any]) : OVertex = {
    val new_elt = db.newVertex(_class.name)

    props.foreach(tup => new_elt.setProperty(tup._1, tup._2))

    new_elt.save()
  }
}