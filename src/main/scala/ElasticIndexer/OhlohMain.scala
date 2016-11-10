package ElasticIndexer

import java.io.File

import MyServer.ProjectData
import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.json4s._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.xml.Elem

/**
  * Created by titusjuocepis on 11/3/16.
  */
object OhlohMain extends App {

  import akka.pattern.ask // ask needs a dispatcher

  implicit val system = ActorSystem("Ohloh") // The actor system
  implicit val materializer = ActorMaterializer() // The actor materializer
  implicit val timeout: Timeout = 10.seconds // How long to wait for ask() response from an actor
  implicit val formats = DefaultFormats // Used for formatting Json

  //val projects = new util.ArrayList[String]

  // Loops through a range of projects where $i is the project id
  for (i <- 10501 to 10700) {
    val ohlohActor = system.actorOf(Props[OhlohActor]) // Creates the actor to process Ohloh requests
    val future = ask(ohlohActor, Download(i)).mapTo[(Elem,Elem)] // Sends the download request to OhlohActor

    // When the response is received...
    future onSuccess {
      case result : (Elem,Elem) =>

        // Create a metadata object
        val projectMetaData = parseXmlToGetProjectData(result._1)

        // Extract the repository URLs from the result
        val repo_urls: Seq[String] = (result._2 \\ "url").map(_.text.trim)

        // Flag used to check if it is a git repository or not (We are only working with git repositories)
        var isGitRepo = false

        // *** I BELIEVE THAT THIS DOES NOT DO ANYTHING SINCE IT KEEPS RESETING THE FLAG OVER AND OVER ***
        // For each URL check to see it is a git repository and set the isGitRepo flag
        repo_urls foreach { url =>
          if (url.contains(".git")) {
            println("Project ID = " + projectMetaData.id + " DOES have a GIT repository. Including Project Repo!!!")
            isGitRepo = true
          }
          else {
            isGitRepo = false
            println("Project ID = " + projectMetaData.id + " does NOT have a GIT repository. Skipping Project Repo!!!")
          }
        }

        // If it was a git repository...
        if (isGitRepo) {

          // Create an actor and send it a message to clone the git repository
          val gitRepoCloneHelper = system.actorOf(Props[GitRepoCloneHandler])
          val answer = ask(gitRepoCloneHelper, GetProjectContents(repo_urls, projectMetaData.id, projectMetaData)).mapTo[File]

          // When the response is received the repository was cloned...
          answer onSuccess {

            case projectRepoDir =>
              println("Cloned repository: " + projectRepoDir.getName + " into : " + projectRepoDir.getCanonicalPath)
          }
        }
    }
    future onFailure {
      case e : Exception =>
        e.printStackTrace()
      case _ =>
        println("------ Default Case ------")
    }
  }

  def parseXmlToGetProjectData(xml: Elem) : ProjectMeta = {
    // Extract necessary project meta data from the result
    val id = (xml \\ "project_id").text
    val name = (xml \\ "name").text
    val desc = (xml \\ "description").text
    val tags: Seq[String] = (xml \\ "tags" \ "tag").map(_.text)
    val url = (xml \\ "homepage_url").text
    val languages = (xml \\ "language").map(_.text.trim)
    ProjectMeta(id, name, desc, tags.mkString(" "), url)
  }
}
