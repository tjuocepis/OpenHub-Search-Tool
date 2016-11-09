package ElasticIndexer

import java.net.URL

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

import scala.xml.{Elem, XML}

/*
 *  Receives a download request and sends it to an OhlohHelper actor.
 *  It then receives a message with a tuple containing project meta data
 *  and project enlistments containing the repositories.  It sends the tuple
 *  back to OhlohHelper actor to extract the XML data.
 */
class OhlohActor extends Actor with ActorLogging {

  implicit var materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  override def receive: Receive = {
    case Download(i) =>
      val helper = context.actorOf(Props[OhlohHelper])
      helper ! SendURL(getURL(i), sender)
    case SendXML(data, ref) => {
      sender.tell(SendResponse(data), ref)
    }
  }

  /*
   *  Constructs and returns a tuple of URLs for getting
   *  the project meta data and enlistments
   */
  def getURL(i : Int): (URL,URL) = {
    val url1 = new URL("https://www.openhub.net/p/"+i+".xml?api_key=105a75678300f34632cf9b76b282e4728d5218051586ff07827352fd077ebabe")
    val url2 = new URL("https://www.openhub.net/p/"+i+"/enlistments.xml?api_key=105a75678300f34632cf9b76b282e4728d5218051586ff07827352fd077ebabe")
    (url1, url2)
  }
}

/*
 *  Receives a tuple of URLs for project metadata and enlistments.
 *  It then loads the XML from the URLs and sends it back to the
 *  OhlohActor.  It then receives a response from OhlohActor that
 *  contains a tuple of Elem objects for the metadata and enlistments.
 *  It sends that response back to the temp actor created by OhlohMain
 *  for further processing.
 */
class OhlohHelper extends Actor with ActorLogging {

  implicit var materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  override def receive: Receive = {

    case SendURL(url, ref) =>
      val data = XML.load(url._1)
      val enlistments = XML.load(url._2)
      sender ! SendXML((data,enlistments), ref)

    case SendResponse(response) =>
      sender ! response
      //context.stop(context.parent)
      //context.stop(self)
  }
}

// Actor Messages
case class SendResponse(response: (Elem,Elem))
case class SendURL(url: (URL, URL), ref: ActorRef)
case class SendXML(data: (Elem,Elem), ref: ActorRef)
case class Download(i: Int)
