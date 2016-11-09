package ElasticIndexer

import java.net.InetAddress

import akka.actor.{Actor, ActorLogging}
import org.elasticsearch.action.bulk.{BulkRequest, BulkRequestBuilder, BulkResponse}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization._
import org.json4s.DefaultFormats
import org.json4s._


/*
 *  Receives a IndexProject message with a Json string.
 */
class ElasticIndexRequestHandler extends Actor with ActorLogging {

  implicit val formats = DefaultFormats

  override def receive: Receive = {

    case IndexProject(metaData, fileData) =>
      val settings = Settings.builder().put("cluster.name", "elasticsearch-titus").build()
      val client = TransportClient.builder().settings(settings).build()
        .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("104.197.149.99"), 9300))

      val metaJson = write(metaData)

      val response = client.prepareIndex("projects", "project-meta-data").setSource(metaJson).get()
      println("INDEX = " + response.getIndex)
      println("TYPE = " + response.getType)
      println("ID = " + response.getId)
      println("VERSION = " + response.getVersion)

      val bulkRequest = client.prepareBulk()
      fileData foreach { fileObject =>
        val fileJson = write(fileObject)
        bulkRequest.add(client.prepareIndex("projects", "project-file-data").setSource(fileJson))
        println("To BulkRequest added the file: " + fileObject.path)
      }

      val bulkResponse = bulkRequest.get()
      if (bulkResponse.hasFailures)
        println("BulkRequest had failures!!!")
  }
}

case class IndexProject(metaData: ProjectMeta, fileData: Seq[FileData])