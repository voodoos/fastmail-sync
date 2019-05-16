package database.dbWrapper

import com.orientechnologies.orient.core.db.ODatabaseSession
import database.dbWrapper.traits.Schema




//class Mailbox {
//}

object MailBox extends Schema {
  object Props {
    sealed class MailboxProperty(override val name: String) extends Property(name)

    case object Name extends MailboxProperty("name") { type U = String }
    case object Server extends MailboxProperty("server") { type U = String }
    case object Address extends MailboxProperty("address") { type U = String }
    case object Password extends MailboxProperty("password") { type U = String }
  }
  case object Class extends Class(name = "Mailbox")


  // Implements Schema
  override def init_schema(implicit db : ODatabaseSession): Unit = {
    if(!Tools.class_exists(Class)) {

    }
  }
}