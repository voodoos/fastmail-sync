import com.orientechnologies.orient.core.metadata.schema.{OClass, OType}
import com.orientechnologies.orient.core.record.{ODirection, OVertex}
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException
import com.sun.mail.imap.{IMAPFolder, IMAPMessage}
import javax.mail.Folder

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Using}

class ODBMail(config : DBConfig, clean : Boolean = false) extends ODBWrapper(config, clean) {

  def getMailBox(mail:  String) : OVertex = {
    val query = "SELECT * from Mailbox where address = ? LIMIT 1"
    Using(db.query(query, mail)) {
      results => results.next()
    } match {
      case Success(value) => value.getVertex.get()
      case Failure(exception) => throw exception //todo nice exception
    }
  }

  private def getSubFolders(parent: OVertex) = {
    parent.getVertices(ODirection.IN, "SubFolder").asScala.map(fv => new DBWrapper.Folder(fv))
  }

  /**
    * One way sync of folders: IMAP => DB
    * @param folders the root folders
    * @param parent  the parent mailbox
    *
    * todo: moved folders are deleted and recreated, this can be optimized
    */
  def sync_folders(folders: Array[Folder], parent : OVertex, first : Boolean = true) : Unit = {
    if (first) Debug.info("Folder synchronisations for " + config.user)

    // We get the list of all the database folders at this level
    // when a folder is find on the server and the database
    // it is removed from the list.
    // Remaining folders in the list are not on the server anymore,
    // we delete them.
    var knownFolders : Iterable[DBWrapper.Folder] = getSubFolders(parent)

    folders.foreach(f => {
      //if (first) Debug.info("In folder " + f.getFullName)

      // If not already in we add the folder to the database
      // Folder are uniques by FullName + UIDValidity
      var vf : OVertex = null
      try {
        // Folder creation will fail if it is a duplicate
        //Debug.info("Attempt to create folder: " + f.getFullName)
        vf = DBWrapper.Folder.create_from_IMAPFolder(f.asInstanceOf[IMAPFolder]).getVertex
        Debug.info("New folder: " + f.getFullName)

        // Set the new folder parent
        vf.addEdge(parent, "SubFolder").save()

        // Fetch mails of this folder
        fetch_mails(f.asInstanceOf[IMAPFolder], vf)
      } catch {
        // If folder already in database we get its rid
        case e: ORecordDuplicatedException =>
          Debug.info("Duplicate folder " + f.getFullName + " not created.")

          // We remove the folder from the known folders list
          knownFolders = knownFolders.filter(of => {
            !(
              of.FullName == f.getFullName &&
                of.UIDValidity == f.asInstanceOf[IMAPFolder].getUIDValidity)
          })

          vf = db.getRecord(e.getRid) // The exception gives us the original record
      }

      // Recursive call:
      val l = f.list()
      if (l.nonEmpty) sync_folders(l, vf, first = false)
      else {
        // If No more imap subfodlers, we check that there is no more db subfolders
        getSubFolders(vf).foreach(wf => {
          Debug.info("Delete: " + wf.FullName)
          wf.deleteWithMessages()
          ()
        })
      }
    })

    // Delete missing folders:
    knownFolders.foreach(ov => {
      Debug.info("Delete: " + ov.FullName)
      ov.deleteWithMessages()
      ()
    })
  }

def fetch_mails(folder: IMAPFolder, vfolder: OVertex): Unit = {
  Debug.info("Fetching mail in folder " + folder.getFullName + ".")
  folder.open(Folder.READ_ONLY)
  folder.getMessages.foreach(m =>
    try{
      DBWrapper.Message.create_from_IMAPMessage(m.asInstanceOf[IMAPMessage], Some(vfolder))
    } catch {
      case e: ORecordDuplicatedException =>
        Debug.warn("Duplicate mail: " + e.getMessage)
    }
  )
  folder.close()
}

override protected def init_schema(): Unit = {
  val mailbox = createVClass("Mailbox")
  val folder = createVClass("Folder")
  val message = createVClass("Message")

  val subfolder = createEClass("SubFolder")
  val in = createEClass("In")

  createProperty(mailbox, "name", OType.STRING)
  createProperty(mailbox, "address", OType.STRING, Some(OClass.INDEX_TYPE.UNIQUE))
  createProperty(mailbox, "server", OType.STRING)
  createProperty(mailbox, "password", OType.STRING)

  createProperty(folder, "name", OType.STRING, Some(OClass.INDEX_TYPE.NOTUNIQUE))
  createProperty(folder, "full_name", OType.STRING)
  createProperty(folder, "uid_validity", OType.INTEGER)

  if(folder.getClassIndex("FullName_UIDValidity_index") == null)
    folder.createIndex("FullName_UIDValidity_index", OClass.INDEX_TYPE.UNIQUE, "full_name", "uid_validity")


  createProperty(message, "UID", OType.INTEGER)//, Some(OClass.INDEX_TYPE.UNIQUE))
  createProperty(message, "headers", OType.CUSTOM)
  createProperty(message, "flags", OType.CUSTOM)
  createProperty(message, "subject", OType.STRING, Some(OClass.INDEX_TYPE.FULLTEXT))
  createProperty(message, "body", OType.STRING, Some(OClass.INDEX_TYPE.FULLTEXT))

  /* TESTS */
  DBWrapper.Init.setTestData()
}
}

