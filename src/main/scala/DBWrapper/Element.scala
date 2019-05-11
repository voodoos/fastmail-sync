package DBWrapper

import com.orientechnologies.orient.core.db.ODatabaseSession
import com.orientechnologies.orient.core.record.{OElement, OVertex}
import com.orientechnologies.orient.core.record.impl.ODocument

abstract class Element(private val element : OElement, val _class: Class) {
  def getProperty(prop : Property) : prop.U = {
    element.getProperty(prop.name).asInstanceOf[prop.U]
  }

  def delete() : Unit = element.delete()

  def class_exists(db : ODatabaseSession) : Boolean = {
    db.getClass(_class.name) != null
  }

  def init_schema(implicit db : ODatabaseSession) : Unit
}
