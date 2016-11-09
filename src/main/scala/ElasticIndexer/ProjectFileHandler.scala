package ElasticIndexer

import java.io.File

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization

import scala.io.Source
import scala.util.control.Breaks._

/*
 *  Receives a ProduceProject message with the repository
 *  root directory, extracts the inner directories and for
 *  each one of them it creates a list of RepoModule objects.
 *  It then creates a Project object from the RepoModule list
 *  and converts it into Json and sends it to ElasticIndexRequestHandler
 *  actor. Project object contains the metadata as well as all the
 *  source files.
 */
class ProjectFileHandler extends Actor with ActorLogging {

  implicit var materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  implicit val formats = Serialization.formats(NoTypeHints)

  override def receive: Receive = {

    case ProduceProject(dir, metaData) =>
      var fileObjects = Seq.empty[FileData]
      val files = getRecursiveListOfFiles(dir)

      files foreach { file =>
        breakable {
          // Filter out all the directories, .git directories and hidden files/directories
          if (!file.getAbsolutePath.contains(".git") && file.isFile && !file.isHidden) {
            var fileData: FileData = null.asInstanceOf[FileData]
            try {
              fileData = makeFileDataObject(metaData, file)
            } catch {
              case e: Exception =>
                println("Weird file! Skipping...") // If the file is not UTF-8 format e.g. blah.png or blah.cnx it skips it
                break
            }
            fileObjects = fileObjects :+ fileData
          }
        }
      }

      val elasticIndexHandler = context.actorOf(Props[ElasticIndexRequestHandler])
      elasticIndexHandler ! IndexProject(metaData, fileObjects)
  }

  // Recursively gets a list of all the contents in a directory
  def getRecursiveListOfFiles(dir: File): Seq[File] = {
    val these = dir.listFiles
    val filtered = these ++ these.filter(_.isDirectory).flatMap(getRecursiveListOfFiles)
    filtered.toSeq
  }

  // Removes common reserved language keywords from a file
  def removeKeywords(fileContents: String) : String = {
    // separators to split the file contents by
    val separators = Array(' ','\n','\r','\t','\"','\'',')','(','=','+','-','*','/','\\','.','#', '!', '|', '&', '<', '>', '?', ':', ';', '[', ']', '{', '}', ',')

    val array = fileContents.split(separators)

    val set = array.toSet.map { item: String =>
      item.toLowerCase
    }

    // Remove common reserved language keywords from set
    val newSet = set - ("for", "while", "do", "import", "return", "void", "string", "int", "double", "float", "bool", "boolean", "val",
      "var", "byte", "class", "object", "seq", "set", "array", "map", "list", "extends", "implements", "with", "type",
      "true", "false", "if", "else", "let", "or", "this", "new", "char", "default", "break", "case", "const", "continue",
      "enum", "extern", "goto", "long", "register", "short", "signed", "sizeof", "static", "struct", "switch", "typedef",
      "union", "unsigned", "volatile", "_Packed", "abstract", "private", "public", "protected", "throws", "assert",
      "instanceof", "native", "static", "synchronized", "transient", "finally", "strictfp", "interface", "package",
      "super", "throw", "except", "def", "del", "elif", "from", "global", "in", "is", "lambda", "not", "pass", "print",
      "try", "catch", "raise", "yield")

    newSet.mkString(" ")
  }

  // Creates a FileModule object
  def makeFileDataObject(meta: ProjectMeta, file: File) : FileData = {
    val fileContents = Source.fromFile(file).mkString
    val filteredFileContents = removeKeywords(fileContents)
    FileData(file.getName, file.getPath, filteredFileContents, meta.id, meta.project_name, meta.url)
  }
}

// Actor Messages
case class ReadFiles(dir: File)
case class ProduceProject(dir: File, metaData: ProjectMeta)

// Project data models
case class FileData(filename: String, path: String, content: String, project_id: String, project_name: String, project_url: String)
case class ProjectMeta(id: String, project_name: String, description: String, tags: String, url: String)
