package MyServer

import java.net.InetAddress

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.json4s._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import scala.io.StdIn

object Server extends App {

  implicit val system = ActorSystem("My-Actors")
  // initializing Actor system
  implicit val materializer = ActorMaterializer()
  // initializing Materializer
  implicit val timeout: Timeout = 5.seconds // setting ask() timeout

  implicit val formats = DefaultFormats

  val settings = Settings.builder().put("cluster.name", "elasticsearch-titus").build()
  val client: TransportClient = TransportClient.builder().settings(settings).build()
    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("104.197.88.210"), 9300))

  /*
   *  GET endpoint
   *  ex) http://localhost:8080/elastic?where=tags&keyword=html&results=100
   *      OR
   *      http://104.198.215.5:9999/elastic?where=code&keyword=engine
   *  $where accepts 'id', 'project_name', 'description', 'url', 'tags', 'code' as its values e.g. where=code
   *  if where is not specified it will default to searching through everything
   *  $keyword accepts any word e.g. keyword=Java
   *  if keyword is not specified it will default to c++
   *  $results accepts any number value e.g. results=100
   *  if results is not specified it defaults to 10000 to return max amount of results
   */
  def route: Route = {
    (path("elastic") & get) {
      parameter("keyword" ? "c++", "where" ? "_all", "results" ? 10000) { (keyword, where, results) =>

        val actor = system.actorOf(Props[ElasticRequestHandler])

        // Receives a MyServer.NeatResponse and sends the text back to the client
        onSuccess(ask(actor, RemoteElasticQuery(keyword, where, results, client)).mapTo[NeatResponse]) { resp: NeatResponse =>
          actor ! PoisonPill
          complete(resp.text) // return response to client
        }
      }
    }
  }

  /*
   *  creates an Actor and sends it a query message specifying to query a remote elasticsearch instance
   */
  def sendQuery(query: RemoteElasticQuery, actor: ActorRef): Future[NeatResponse] = {
    ask(actor, query).mapTo[NeatResponse]
  }

  /*
   *  creates an Actor and sends it a query message specifying to query a local elasticsearch instance
   */
  def sendQuery(query: LocalElasticQuery): Future[NeatResponse] = {
    val actor = system.actorOf(Props[ElasticRequestHandler])
    ask(actor, query).mapTo[NeatResponse]
  }

  // START SERVER
  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 9999)

  //println("ENTER to terminate")
  //StdIn.readLine()
  //system.terminate()
}