package ElasticIndexer

import java.io.File

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

        // Extract necessary project meta data from the result
        val id = (result._1 \\ "project_id").text
        val name = (result._1 \\ "name").text
        val desc = (result._1 \\ "description").text
        val tags: Seq[String] = (result._1 \\ "tags" \ "tag").map(_.text)
        val url = (result._1 \\ "homepage_url").text
        val languages = (result._1 \\ "language").map(_.text.trim)

        // Create a metadata object
        val projectMetaData = ProjectMeta(id, name, desc, tags.mkString(" "), url)

        // Extract the repository URLs from the result
        val repo_urls: Seq[String] = (result._2 \\ "url").map(_.text.trim)

        // Flag used to check if it is a git repository or not (We are only working with git repositories)
        var isGitRepo = false

        // For each URL check to see it is a git repository and set the isGitRepo flag
        repo_urls foreach { url =>
          if (url.contains(".git")) {
            println("Project ID = " + id + " DOES have a GIT repository. Including Project Repo!!!")
            isGitRepo = true
          }
          else {
            isGitRepo = false
            println("Project ID = " + id + " does NOT have a GIT repository. Skipping Project Repo!!!")
          }
        }

        // If it was a git repository...
        if (isGitRepo) {

          // Create an actor and send it a message to clone the git repository
          val gitRepoCloneHelper = system.actorOf(Props[GitRepoCloneHandler])
          val answer = ask(gitRepoCloneHelper, GetProjectContents(repo_urls, id, projectMetaData)).mapTo[File]

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
}
