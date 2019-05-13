package database.dbWrapper

import com.orientechnologies.orient.core.db.ODatabaseSession

object Tools {
  def class_exists(_class : Class)(implicit db : ODatabaseSession) : Boolean = {
    db.getClass(_class.name) != null
  }
}
