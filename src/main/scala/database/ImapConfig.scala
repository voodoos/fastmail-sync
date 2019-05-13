package database

object ImapConfig {
  sealed trait ImapProtocol
  case object IMAP extends ImapProtocol
  case object IMAPS extends ImapProtocol

  case class Config(server : String, user : String, pass : String, protocol : ImapProtocol)
}
