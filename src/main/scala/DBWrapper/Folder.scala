package DBWrapper

import com.orientechnologies.orient.core.db.ODatabaseSession
import com.orientechnologies.orient.core.record.{ODirection, OVertex}
import com.sun.mail.imap.IMAPFolder

import collection.JavaConverters._
import scala.util.{Failure, Success, Using}

object FolderProperties {
  sealed class FolderProperty(override val name: String) extends Property(name)

  case object FullName extends FolderProperty("full_name") {
    type U = String
  }
  case object UIDValidity extends FolderProperty("uid_validity") {
    type U = Integer
  }
}

case object FolderClass extends Class(name = "Folder")

class Folder(private val folder_vertex : OVertex) extends Vertex(folder_vertex, FolderClass) {

  def FullName: String = {
    getProperty(FolderProperties.FullName)
  }

  def UIDValidity: Integer = {
    getProperty(FolderProperties.UIDValidity)
  }

  def getMessages: Iterable[Message] = {
    vertex.getVertices(ODirection.IN, "In").asScala.map(m => new Message(m))
  }

  def deleteWithMessages(): Unit = {
    getMessages.foreach(m => m.delete())
    super.delete()
  }

  override def init_schema(implicit db : ODatabaseSession): Unit = {
    if(!class_exists(db)) {

    }
  }
}

object Folder {
  def get(full_name : FolderProperties.FullName.U, uid_validity : FolderProperties.UIDValidity.U)(implicit db : ODatabaseSession) : Option[Folder] = {
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
}