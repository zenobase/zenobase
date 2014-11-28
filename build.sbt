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
	"org.elasticsearch" % "elasticsearch" % "1.4.1",
	"org.elasticsearch" % "elasticsearch-cloud-aws" % "2.4.1",
	"com.hazelcast" % "hazelcast" % "3.3.3",
	"com.hazelcast" % "hazelcast-cloud" % "3.3.3",
	"org.jscience" % "jscience" % "4.3.1",
	"org.jsoup" % "jsoup" % "1.7.3",
	"net.sf.opencsv" % "opencsv" % "2.3",
	"org.scribe" % "scribe" % "1.3.3",
	"com.braintreepayments.gateway" % "braintree-java" % "2.37.0",
	"org.logback-extensions" % "logback-ext-loggly" % "0.1.2",
	"ch.qos.logback.contrib" % "logback-json-classic" % "0.1.2",
	"ch.qos.logback.contrib" % "logback-jackson" % "0.1.2",
	"org.seleniumhq.selenium" % "selenium-chrome-driver" % "2.39.0" % "test", // Must match Play's version of Selenium!
	"org.mockito" % "mockito-core" % "1.9.5" % "test",
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

pipelineStages := Seq(uglify, gzip)

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

EclipseKeys.withSource := true
