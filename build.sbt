name := "cs441_hw3_server"

version := "1.0"

scalaVersion := "2.11.0"

libraryDependencies ++= Seq(
  // jackson
  //"com.fasterxml.jackson.core" % "jackson-core" % "2.8.0",
  "org.json4s" %% "json4s-native" % "3.5.0",
  "org.json4s" %% "json4s-jackson" % "3.5.0",
  // xml
  "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
  // akka
  "com.typesafe.akka" %% "akka-actor" % "2.4.12",
  //"com.typesafe.akka" %% "akka-testkit" % "2.4.12" % "test",
  // streams
  //"com.typesafe.akka" %% "akka-stream" % "2.4.12",
  // akka http
  "com.typesafe.akka" %% "akka-http-core" % "2.4.11",
  //"com.typesafe.akka" %% "akka-http-experimental" % "2.4.11",
  //"com.typesafe.akka" %% "akka-http-jackson-experimental" % "2.4.11",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.11",
  //"com.typesafe.akka" %% "akka-http-xml-experimental" % "2.4.11",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  //elastic
  //"com.sksamuel.elastic4s" %% "elastic4s-core" % "2.4.0",
  "org.slf4j" % "slf4j-simple" % "1.7.21",
  "org.apache.logging.log4j" % "log4j-api" % "2.6.2",
  "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.6.2",
  //"org.elasticsearch.client" % "transport" % "5.0.0",
  "org.elasticsearch" % "elasticsearch" % "2.4.0"
  //"org.asynchttpclient" % "async-http-client" % "2.0.0-RC18",
  //"io.netty" % "netty-all" % "4.1.1.Final",
  //"org.apache.servicemix.bundles" % "org.apache.servicemix.bundles.lucene-queries" % "6.0.1_1",
  //"org.apache.servicemix.bundles" % "org.apache.servicemix.bundles.elasticsearch" % "2.1.1_2"
  //"org.apache.servicemix.bundles" % "org.apache.servicemix.bundles.lucene-queries" % "5.3.1_1",
  //"io.netty" % "netty-buffer" % "4.0.0.Beta2",
  //"org.apache.httpcomponents" % "httpclient" % "4.2.1"
)

resolvers += "OSS Sonatype" at "https://repo1.maven.org/maven2/"