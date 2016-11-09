package ElasticIndexer

import java.io.File

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, duration, _}
import scala.sys.process.Process

/*
 *  Receives a message to get project contents from the git repository.
 *  It then sends a message to GitRepoCloneHelper for each repository
 *  that it received.  It then waits for those actors to clone each
 *  of the git repositories.  It receives a DoneCloning message from
 *  each of the actors for each cloned repository.  Once all the
 *  DoneCloning messages are received it sends a message to
 *  ProjectFileHandler actor with a cloned repository directory
 *  for extracting file contents
 */
class GitRepoCloneHandler extends Actor with ActorLogging {

  implicit var materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  var origSender: ActorRef = _ // Will contain a handle on the original sending actor
  var requestsSent = 0 // Keeps track of requests sent to actors
  var responsesReceived = 0 // Keeps track of responses received from actors
  var projectMetaData: ProjectMeta = _ // Will contain project metadata once extracted

  override def receive: Receive = {

    case GetProjectContents(gitRepos, projectId, projectMeta) =>
      origSender = sender
      projectMetaData = projectMeta
      gitRepos foreach { repo =>
        val cloneHelper = context.actorOf(Props[GitRepoCloneHelper])
        cloneHelper ! CloneRepo(repo, projectId)
        requestsSent += 1
      }

    case DoneCloning(projectDir, repoName) =>
      responsesReceived += 1
      if (requestsSent == responsesReceived) {
        println("ALL RESPONSES RECEIVED!")
        val fileHandler = context.actorOf(Props[ProjectFileHandler])
        fileHandler ! ProduceProject(projectDir, projectMetaData)
      }
  }
}

/*
 *  Receives a CloneRepo message with the git repository url.
 *  It then clones the repository locally into ../git_repos
 *  directory and sends the repository directory back to
 *  the GitRepoCloneHandler actor.
 */
class GitRepoCloneHelper extends Actor with ActorLogging {

  override def receive: Receive = {

    case CloneRepo(gitRepo, projectId) =>
      val repoNameSplit = gitRepo.split("\\w+://\\w+\\.com/\\w+/")
      val repoName = repoNameSplit(1).replace(".git", "")
      val projectDir = cloneRepo(gitRepo, projectId)
      sender ! DoneCloning(projectDir, repoName)
  }

  /*
   *  Clones the git repository based on the url and
   *  returns the directory
   */
  def cloneRepo(git_repo: String, id: String) : File = {
    val projectDir = new File("git_repos/" + id)
    var projectDirExists = projectDir.exists()

    if (!projectDirExists) {
      projectDir.mkdirs()
      projectDirExists = projectDir.exists()
    }

    if (projectDirExists) {
      val output = Process(Seq("git", "clone", "--depth", "1", git_repo), projectDir)
      val process = output.run() // start asynchronously
      val future = Future(blocking(process.exitValue())) // wrap in Future

      val res = try {
        val result = Await.result(future, duration.Duration(20, "sec"))
        if (result == 0) {
          println("Cloning SUCCEEDED!")
          process.destroy()
          process.exitValue()
        }
        else {
          println("Cloning FAILED!")
          process.destroy()
          process.exitValue()
        }
      } catch {
        case _: TimeoutException =>
          println("Cloning TIMED OUT!")
          process.destroy()
          process.exitValue()
      }
    }
    else {
      println("Failed to create directory...")
    }

    projectDir
  }
}

// Actor Messages
case class GetProjectContents(gitRepos: Seq[String], id: String, meta: ProjectMeta)
case class CloneRepo(gitRepo: String, projectId: String)
case class DoneCloning(projectDir: File, repoName: String)