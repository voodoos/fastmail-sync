import java.net.SocketException

import com.sun.mail.imap.IMAPFolder
import javax.mail.Folder
import javax.mail.event.{MessageChangedEvent, MessageCountAdapter, MessageCountEvent}

import scala.util.Using

class ImapIDLE(val client : ImapAccount, val folder: Folder) extends Runnable {

  override def run(): Unit = {
    // RAII
    Using(client.new_connection()) {
      store =>
        require(store.hasCapability("IDLE"), "Server does not support IDLE")

        Using(store.getFolder(folder.getName).asInstanceOf[IMAPFolder]) {
          inbox =>
            inbox.open(Folder.READ_ONLY)

            inbox.addMessageChangedListener((messageChangedEvent: MessageChangedEvent) => {
              println("messagechanged")
            })
            inbox.addMessageCountListener(new MessageCountAdapter() {
              override def messagesAdded(ev: MessageCountEvent): Unit = {
                val folder = ev.getSource.asInstanceOf[Folder]
                val msgs = ev.getMessages
                System.out.println("Folder: " + folder + " got " + msgs.length + " new messages")
                // process new messages
                //          idleManager.watch(folder) // keep watching for new messages

                // handle exception related to the Folder
              }
            })
            try {
              inbox.idle()
            }
            catch {
              case e: SocketException => {
                println("arg: " ++ e.getMessage)
                run()
              }
            }
        }
    }
  }
}
