package DBWrapper

import com.orientechnologies.orient.core.db.ODatabaseSession
import com.orientechnologies.orient.core.record.OVertex
import com.sun.mail.imap.{IMAPFolder, IMAPMessage}


object MessageProperties {
  sealed class MessageProperty(override val name: String) extends Property(name)

  case object Subject extends MessageProperty("subject") {
    type U = String
  }
  case object UID extends MessageProperty("uid") {
    type U = Integer
  }
}

case object MessageClass extends Class(name = "Message")

class Message(private val message_vertex : OVertex) extends Vertex(message_vertex, MessageClass) {
  def set_folder(f : Folder) ={
    message_vertex.addEdge(f.getVertex, "In").save()
    ()
  }

  override def init_schema(implicit db : ODatabaseSession): Unit = {
  //  List(33,4).foreach()
  }
}

object Message {
  def create_from_IMAPMessage(message : IMAPMessage, fvtx : Option[OVertex] = None)(implicit db : ODatabaseSession) : Message = {
    val folder = message.getFolder.asInstanceOf[IMAPFolder]
    val props = Map(
      MessageProperties.Subject.name -> message.getSubject,
      MessageProperties.UID.name -> folder.getUID(message))

    // Don't query for the folder if it is given as an optional argument
    val f2 = fvtx match {
      case Some(fv) => Some(new Folder(fv))
      case None => Folder.get(folder.getFullName, folder.getUIDValidity.toInt)
    }
    f2 match {
      case Some (f) => {
        val new_message = new Message(Vertex.create(db, MessageClass, props))
        new_message.set_folder(f)
        new_message
      }
      case None => throw new RuntimeException("Mail folder not in the database")
    }
  }
}
