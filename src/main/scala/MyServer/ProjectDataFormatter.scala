package MyServer

import ElasticIndexer.ProjectMeta
import akka.actor.{Actor, ActorLogging}

/*
 *  Receives a list of Projects and a total number of results.
 *  It then formats each Project into a nicely legible String
 *  and sends it back to the original Actor that initiated the
 *  client's request
 */
class ProjectDataFormatter extends Actor with ActorLogging {

  override def receive: Receive = {

    case ProjectData(projects, totalHits) =>
      var text = "---[Start of Results]---\n\n"
      var projectNumber = 1

      projects foreach { data =>
        text = text.concat(projectToText(data, projectNumber))
        projectNumber += 1
      }
      text = text.concat("Displaying " + projects.size + " results\n\n")
      text = text.concat("----[End of Results]----\n\n")

      sender ! NeatResponse(text)

    case CodeData(codeData, totalHits) =>
      var text = "---[Start of Results]---\n\n"
      text = text.concat("Listing matched files...\n\n")
      var fileNumber = 1

      codeData foreach { data =>
        text = text.concat(codeDataToText(data, fileNumber))
        fileNumber += 1
      }

      text = text.concat("Displaying " + codeData.size + " results\n\n")
      text = text.concat("----[End of Results]----\n\n")

      sender ! NeatResponse(text)
  }

  def codeDataToText(data: CodeMeta, i: Int): String = {
    var tempString = "------[File "+i+"]------\n"
    tempString = tempString.concat("FILE: " + data.path + "\n")
    tempString = tempString.concat("---------------------\n")
    tempString = tempString.concat("Belongs to project:\n")
    tempString = tempString.concat("PROJECT ID   : " + data.id + "\n")
    tempString = tempString.concat("PROJECT NAME : " + data.name + "\n")
    tempString = tempString.concat("PROJECT URL  : " + data.url + "\n")
    tempString = tempString.concat("========================\n\n")
    tempString
  }

  /*
   *  Formats a ElasticIndexer.Project into nicely legible String
   */
  def projectToText(p: ProjectMeta, i: Int): String = {
    var tempString = "------[Project "+i+"]------\n"
    tempString = tempString.concat("ID          : " + p.id + "\n")
    tempString = tempString.concat("NAME        : " + p.project_name + "\n")
    tempString = tempString.concat("URL         : " + p.url + "\n")
    tempString = tempString.concat("DESCRIPTION : " + p.description + "\n")
    tempString = tempString.concat("TAGS        : " + p.tags + "\n")
    tempString = tempString.concat("========================\n\n")
    tempString
  }
}

case class NeatResponse(text: String)