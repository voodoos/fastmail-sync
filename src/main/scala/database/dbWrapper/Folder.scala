package database.dbWrapper

import com.orientechnologies.orient.core.db.ODatabaseSession
import com.orientechnologies.orient.core.record.{ODirection, OVertex}
import com.sun.mail.imap.IMAPFolder

import traits._

import collection.JavaConverters._
import tools.{Failure, Success, Using}




class Folder(private val folder_vertex : OVertex)
  extends Vertex(folder_vertex) {

  def FullName: String = {
    getProperty(Folder.Props.FullName)
  }

  def UIDValidity: Integer = {
    getProperty(Folder.Props.UIDValidity)
  }

  def getMessages: Iterable[Message] = {
    vertex.getVertices(ODirection.IN, "In").asScala.map(m => new Message(m))
  }

  def deleteWithMessages(): Unit = {
    getMessages.foreach(m => m.delete())
    super.delete()
  }

}

object Folder extends Schema {
  object Props {
    sealed class FolderProperty(override val name: String) extends Property(name)

    case object FullName extends FolderProperty("full_name") { type U = String }
    case object UIDValidity extends FolderProperty("uid_validity") { type U = Integer }
  }
  case object Class extends Class(name = "Folder")

  def get(full_name : Props.FullName.U, uid_validity : Props.UIDValidity.U)(implicit db : ODatabaseSession) : Option[Folder] = {
    val query = "SELECT * from Folder where full_name = ? and uid_validity = ? LIMIT 1"
    Using(db.query(query, full_name,  uid_validity)) {
      results => results.next()
    } match {
      case Success(value) => Some(new Folder(value.getVertex.get()))
      case Failure(_) => None
    }
  }

  def create_from_IMAPFolder(f : IMAPFolder)(implicit db : ODatabaseSession) : Folder = {
    val vf = db.newVertex("Folder")
    vf.setProperty("name", f.getName)
    vf.setProperty("full_name", f.getFullName)
    vf.setProperty("uid_validity", f.getUIDValidity)
    vf.save()
    new Folder(vf)
  }

  // Implements Schema
  override def init_schema(implicit db : ODatabaseSession): Unit = {
    if(!Tools.class_exists(Class)) {

    }
  }
}