import scala.concurrent.duration._

name := "zenobase"

version := "SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava).enablePlugins(SbtWeb)

scalaVersion := "2.11.1"

resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"

libraryDependencies ++= Seq(
	javaWs,
	"javax.mail" % "mail" % "1.4.7",
	"com.google.guava" % "guava" % "17.0",
	"com.google.guava" % "guava-testlib" % "17.0" % "test",
	"com.google.inject" % "guice" % "3.0",
	"com.google.inject.extensions" % "guice-multibindings" % "3.0",
	"org.elasticsearch" % "elasticsearch" % "1.1.1",
	"org.elasticsearch" % "elasticsearch-cloud-aws" % "2.1.1",
	"org.jscience" % "jscience" % "4.3.1",
	"org.jsoup" % "jsoup" % "1.7.3",
	"net.sf.opencsv" % "opencsv" % "2.3",
	"org.scribe" % "scribe" % "1.3.3",
	"com.braintreepayments.gateway" % "braintree-java" % "2.29.1",
	"org.seleniumhq.selenium" % "selenium-chrome-driver" % "2.39.0" % "test", // Must match Play's version of Selenium!
	"org.apache.httpcomponents" % "httpcore" % "4.3.2" % "test",
	"org.mockito" % "mockito-core" % "1.9.5" % "test",
	"org.jvnet.mock-javamail" % "mock-javamail" % "1.9" % "test"
)

JsTaskKeys.timeoutPerSource := 10.minutes

LessKeys.compress := true

UglifyKeys.output := "js/zeno.min.js"

includeFilter in (Assets, LessKeys.less) := "zeno.less"

includeFilter in uglify := "zeno.js"

pipelineStages := Seq(uglify, gzip)
