import scala.concurrent.duration._

name := "zenobase"

version := "SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava).enablePlugins(SbtWeb)

scalaVersion := "2.11.1"

resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"

libraryDependencies ++= Seq(
	javaWs,
	filters,
	"javax.mail" % "mail" % "1.4.7",
	"joda-time" % "joda-time" % "2.10.5",
	"commons-codec" % "commons-codec" % "1.14",
	"com.google.guava" % "guava" % "17.0",
	"com.google.guava" % "guava-testlib" % "17.0" % "test",
	"com.google.inject" % "guice" % "3.0",
	"com.google.inject.extensions" % "guice-multibindings" % "3.0",
	"org.elasticsearch" % "elasticsearch" % "1.7.6",
	"org.elasticsearch" % "elasticsearch-cloud-aws" % "2.7.1",
	"org.jscience" % "jscience" % "4.3.1",
	"org.jsoup" % "jsoup" % "1.15.3",
	"net.sf.opencsv" % "opencsv" % "2.3",
	"org.scribe" % "scribe" % "1.3.5",
	"com.amazonaws" % "aws-java-sdk-core" % "1.10.77",
	"com.amazonaws" % "aws-java-sdk-ec2" % "1.10.77",
	"com.amazonaws" % "aws-java-sdk-kms" % "1.10.77",
	"com.amazonaws" % "aws-java-sdk-s3" % "1.10.77",
	"com.amazonaws" % "aws-java-sdk-ses" % "1.10.77",
	"org.apache.httpcomponents" % "httpcore" % "4.3",
	"org.apache.httpcomponents" % "httpclient" % "4.3.1",
	"org.apache.httpcomponents" % "fluent-hc" % "4.3.1",
	"com.braintreepayments.gateway" % "braintree-java" % "2.109.0",
	"com.fasterxml.jackson.core" % "jackson-core" % "2.13.2",
	"com.fasterxml.jackson.core" % "jackson-databind" % "2.13.2.2",
	"com.fasterxml.jackson.core" % "jackson-annotations" % "2.13.2",
	"org.logback-extensions" % "logback-ext-loggly" % "0.1.5",
	"ch.qos.logback.contrib" % "logback-json-classic" % "0.1.5",
	"ch.qos.logback.contrib" % "logback-jackson" % "0.1.5",
	"org.seleniumhq.selenium" % "selenium-chrome-driver" % "2.39.0" % "test", // Must match Play's version of Selenium!
	"org.mockito" % "mockito-core" % "1.10.19" % "test",
	"org.jvnet.mock-javamail" % "mock-javamail" % "1.9" % "test"
)

fork in Test := false

sources in (Compile, doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false

JsTaskKeys.timeoutPerSource := 10.minutes

LessKeys.compress := true

UglifyKeys.uglifyOps := { js =>
	Seq((js.sortBy(_._2), "js/zeno.js"))
}

UglifyKeys.sourceMap := false

includeFilter in (Assets, LessKeys.less) := "zeno.less"

includeFilter in uglify := "zeno.js"

includeFilter in filter := "*.less"

includeFilter in gzip := "*.html" || "*.css" || "*.js" || "*.json"

pipelineStages := Seq(uglify, filter, gzip)

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
