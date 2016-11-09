package MyServer

import
akka.actor.{Actor, ActorLogging, Props}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.index.query.QueryBuilders
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

/*
 *  Sends a query to remote elasticsearch instance and receives a response.
 *  It then creates an Actor and sends it the response and stops itself.
 */
class ElasticRequestHandler extends Actor with ActorLogging {

  implicit var materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  def receive = {

    case RemoteElasticQuery(keyword, where, results, client) =>

      val senderRef = sender

      if (where.contains("code")) {
        val response = client.prepareSearch()
          .setIndices("projects")
          .setTypes("project-file-data")
          .addFields("path", "project_id", "project_name", "project_url")
          .setQuery(QueryBuilders.multiMatchQuery(keyword, "content")).setSize(results).execute().actionGet()
        val actor = context.actorOf(Props[ElasticResponseHandler])
        actor.tell(ElasticCodeResponse(response), senderRef)
      }
      else {
        val response = client.prepareSearch()
          .setIndices("projects")
          .setTypes("project-meta-data")
          .addFields("id", "project_name", "description", "tags", "url")
          .setQuery(QueryBuilders.multiMatchQuery(keyword, where)).setSize(results).execute().actionGet()
        val actor = context.actorOf(Props[ElasticResponseHandler])
        actor.tell(ElasticResponse(response), senderRef)
      }
  }
}

// Actor Messages
case class LocalElasticQuery(keyword: String, where: String, results: Int)
case class RemoteElasticQuery(keyword: String, wher: String, results: Int, client: TransportClient)
