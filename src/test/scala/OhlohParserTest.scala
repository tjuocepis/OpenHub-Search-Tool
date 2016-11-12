import ElasticIndexer.ProjectMeta

/**
  * Created by titusjuocepis on 11/9/16.
  */
class OhlohParserTest extends org.scalatest.FunSuite {

  test("This contains wrong results!") {
    val xml =
      <response>
        <error></error>
      </response>
    val parsedResult = ElasticIndexer.OhlohMain.parseXmlToGetProjectData(xml)
    assert(parsedResult.id.isEmpty || parsedResult.project_name.isEmpty || parsedResult.description.isEmpty ||
           parsedResult.url.isEmpty || parsedResult.tags.isEmpty)
  }

  test("This contains correct results!") {
    val xml =
      <response>
        <success></success>
        <result>
          <project>
            <project_id>10016</project_id>
            <name>Test Project</name>
            <description>Testing of XML parsing</description>
            <homepage_url>http://testing.edu</homepage_url>
            <tags>
              <tag>test</tag>
              <tag>testing</tag>
              <tag>xml</tag>
            </tags>
          </project>
        </result>
      </response>
    val correctResult = ProjectMeta("10016", "Test Project", "Testing of XML parsing", "test testing xml", "http://testing.edu")
    val parsedResult = ElasticIndexer.OhlohMain.parseXmlToGetProjectData(xml)
    assert(parsedResult.contains(correctResult))
  }
}
