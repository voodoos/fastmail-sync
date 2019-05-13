import database.ImapConfig

import scala.util.{Try, Using}

object FastMail extends App {
  println("Hello, world!")

  //val conf = Config("imap.laposte.net", "thevoodoos@laposte.net", "Rutaba9a", ImapConfig.IMAP)
  //val client =  new ImapAccount(ImapConfig("mail.lotophages.fr", "ulysse@u31.fr", ""))

  /*client.folders().foreach(f => {
    println("F: " ++ f.getFullName)
    try {
     // new Thread(new ImapIDLE(client, f)).start()
    } catch {
      case e: AssertionError =>
        println(e.getMessage)
    }
  })

  val folder = client.new_connection().getFolder("INBOX")
  folder.open(Folder.READ_ONLY)
  folder.getMessages().foreach(m => println(m.getSubject))
*/

  // RAII
   /* def res = Using(new ImapAccount(conf)) {
      imap =>
        def res : Try[Unit] = Using(new ODBMail (DBConfig("remote:localhost","fastmail", "root", "root"), true)) {
          orient => orient.sync_folders(imap.top_folders(), orient.getMailBox(conf.user))
        }
        res match {
          case scala.util.Failure(e) =>
            println("Database error: \n" + e.getMessage)
            throw e
          case _ => ()
        }
    }
  res match {
    case scala.util.Failure(e) =>
      println("Imap error: \n" + e.getMessage)
      throw e
    case _ => ()
  }*/
}
