/**
  * Created by titusjuocepis on 11/9/16.
  */
class OhlohParserTest extends org.scalatest.FunSuite {

  test("should return an error") {
    val xml =
      <response>
        <error></error>
      </response>
    val summaryOpt = ElasticIndexer.OhlohMain.parseXmlToGetProjectData(xml) //OpenHubProjectParser(xmlError)
    assert(summaryOpt.id.isEmpty)
  }
}
