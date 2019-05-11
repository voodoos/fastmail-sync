package DBWrapper


case object MailboxClass extends Class(name = "Mailbox")

object MailboxProperties {
  sealed class MailboxProperty(override val name: String) extends Property(name)

  case object Name extends MailboxProperty("name") {
    type U = String
  }
  case object Server extends MailboxProperty("server") {
    type U = String
  }
  case object Address extends MailboxProperty("address") {
    type U = String
  }
  case object Password extends MailboxProperty("password") {
    type U = String
  }
}

class Mailbox {

}
