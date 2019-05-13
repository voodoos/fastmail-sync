package database

import com.orientechnologies.orient.core.db.{ODatabaseSession, OrientDB, OrientDBConfig}
import com.orientechnologies.orient.core.metadata.schema.{OClass, OProperty, OType}

case class DBConfig(server : String, db : String, user : String, pass : String)

abstract class ODBWrapper(val config: DBConfig, clean : Boolean = false) extends AutoCloseable {
  private val orient = new OrientDB(config.server, OrientDBConfig.defaultConfig())
  implicit protected val db: ODatabaseSession = orient.open(config.db, config.user, config.pass)
  if (clean) reset()
  init_schema()


  override def close(): Unit = if (orient.isOpen) {
    db.close()
    orient.close()
  }

  protected def createVClass(name : String): OClass =
    Option(db.getClass(name)) match {
      case Some(c)  => c
      case None     => db.createVertexClass(name)
    }

  protected def createEClass(name : String): OClass =
    Option(db.getClass(name)) match {
      case Some(c)  => c
      case None     => db.createEdgeClass(name)

    }

  protected def createProperty(oclass : OClass,
                               name : String,
                               typ : OType,
                               index : Option[OClass.INDEX_TYPE] = None
                              ): OProperty = {
    Option(oclass.getProperty(name)) match {
      case Some(p)  => p
      case None     => {
        val p = oclass.createProperty(name, typ)
        index match {
          case Some(i)  => p.createIndex(i)
          case None     => ()
        }
        p
      }
    }
  }

  private def reset(): Unit = {
    db.execute("sql", "DELETE VERTEX V")
    db.execute("sql", "DELETE VERTEX E")
  }

  protected def init_schema(): Unit
}

