import java.util.Properties

import com.sun.mail.imap.{IMAPFolder, IMAPStore}
import javax.mail.{Folder, _}
import javax.mail.event.{MessageChangedEvent, MessageChangedListener, MessageCountAdapter, MessageCountEvent}
import ImapConfig.Config



class ImapAccount(val config : Config) extends AutoCloseable {
  private val protocol = config.protocol match {
    case ImapConfig.IMAP => "imap"
    case ImapConfig.IMAPS => "imaps"
  }

  private val props : Properties = System.getProperties
  props.setProperty("mail.store.protocol", protocol)

  private val session : Session = Session.getDefaultInstance(props, null)
  //session.setDebug(true)

  private val store : IMAPStore = session.getStore(protocol).asInstanceOf[IMAPStore]
  connect()

  @throws(classOf[MessagingException])
  private def connect(): Unit = {
      store.connect(config.server, config.user, config.pass)
  }

  @throws(classOf[MessagingException])
  def new_connection() : IMAPStore = {
    val s = session.getStore(protocol).asInstanceOf[IMAPStore]
    s.connect(config.server, config.user, config.pass)
    s
  }

  def mail_address() : String = config.user

  def top_folders() : Array[Folder] = {
      store.getDefaultFolder.list()
  }

  override def close(): Unit = if (store.isConnected) store.close()

  def bak(): Unit ={

    val props = System.getProperties
    props.setProperty("mail.store.protocol", "imaps")
   // props.setProperty("mail.imap.usesocketchannels", "true") // mail.imaps.usesocketchannels
    val session = Session.getDefaultInstance(props, null)
    val store = session.getStore("imaps")
    try {
      // use imap.gmail.com for gmail
      store.connect("imap.laposte.net", "thevoodoos@laposte.net", "Rutaba9a")
      val inbox = store.getFolder("Inbox").asInstanceOf[IMAPFolder]
      inbox.open(Folder.READ_ONLY)

      inbox.addMessageChangedListener(new MessageChangedListener {
        override def messageChanged(messageChangedEvent: MessageChangedEvent): Unit = {
          println("messagechanged")
        }
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
          inbox.idle()
      //idleManager.watch(inbox)

      /*

      // limit this to 20 message during testing
      val messages = inbox.getMessages()
      val limit = 10
      var count = 0
      for (message <- messages) {
        count = count + 1
        if (count > limit) System.exit(0)
          message.asInstanceOf[IMAPMessage].getAllRecipients.foreach(e => println(e))
      }*/

      inbox.close(true)
    } catch {
      case e: NoSuchProviderException =>  e.printStackTrace()
        System.exit(1)
      case me: MessagingException =>      me.printStackTrace()
        System.exit(2)
    } finally {
      store.close()
    }
}
}
