package dbWrapper

import com.orientechnologies.orient.core.db.ODatabaseSession
import com.orientechnologies.orient.core.record.{OElement, OVertex}
import com.orientechnologies.orient.core.record.impl.ODocument

abstract class Element(private val element : OElement) {
  def getProperty(prop : Property) : prop.U = {
    element.getProperty(prop.name).asInstanceOf[prop.U]
  }

  def delete() : Unit = element.delete()

}
