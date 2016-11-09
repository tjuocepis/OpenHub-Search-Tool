package MyServer

import ElasticIndexer.ProjectMeta
import akka.actor.{Actor, ActorLogging, Props}
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.{SearchHit, SearchHitField}

/*
 *  Receives a response from an Actor and parses it into a collection of ElasticIndexer.Project objects.
 *  It then creates an Actor and sends it the list of projects along with the number of results received.
 */
class ElasticResponseHandler extends Actor with ActorLogging {

  override def receive: Receive = {

    case ElasticResponse(response) =>
      var projectData = Seq.empty[ProjectMeta]
      val hits = response.getHits.getHits
      hits foreach { hit =>
        projectData = projectData :+ getProjectDataFromHit(hit)
      }
      val dataFormatter = context.actorOf(Props[ProjectDataFormatter])
      dataFormatter.tell(ProjectData(projectData, hits.length), sender)

    case ElasticCodeResponse(response) =>
      var codeData = Seq.empty[CodeMeta]
      val hits = response.getHits.getHits
      hits foreach { hit =>
        codeData = codeData :+ getCodeDataFromHit(hit)
      }
      val dataFormatter = context.actorOf(Props[ProjectDataFormatter])
      dataFormatter.tell(CodeData(codeData, hits.length), sender)
  }

  def getCodeDataFromHit(hit: SearchHit) : CodeMeta = {
    val fieldsMap = hit.fields()
    val hitFields = fieldsMap.values()
    val hitFieldsArray = new Array[SearchHitField](hitFields.size())
    val searchHitFields = hitFields.toArray(hitFieldsArray)
    var values = Seq.empty[String]
    searchHitFields foreach { hitField =>
      val value = hitField.getValue[String]
      values = values :+ hitField.getValue[String]
    }
    val v = values.toArray
    CodeMeta(v(0), v(1), v(2), v(3))
  }

  /*
   *  Gets project data from an elastic search hit
   */
  def getProjectDataFromHit(hit: SearchHit) : ProjectMeta = {
    val fieldsMap = hit.fields()
    val hitFields = fieldsMap.values()
    val hitFieldsArray = new Array[SearchHitField](hitFields.size())
    val searchHitFields = hitFields.toArray(hitFieldsArray)
    var values = Seq.empty[String]
    searchHitFields foreach { hitField =>
      val value = hitField.getValue[String]
      values = values :+ hitField.getValue[String]
    }
    val v = values.toArray
    ProjectMeta(v(1), v(2), v(0), v(4), v(3))
  }
}

case class CodeMeta(path: String, url: String, name: String, id: String)

// Actor Messages
case class ElasticResponse(response: SearchResponse)
case class ElasticCodeResponse(response: SearchResponse)
case class ProjectData(projects: Seq[ProjectMeta], totalHits: Long)
case class CodeData(codeData: Seq[CodeMeta], totalHits: Long)



