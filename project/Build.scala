import sbt._
import Keys._
import play.Project._
import com.google.javascript.jscomp.CompilerOptions
import com.google.javascript.jscomp.CompilationLevel

object ApplicationBuild extends Build {

	val appName = "zenobase"
	val appVersion = "SNAPSHOT"

	val appDependencies = Seq(
	  javaCore,
	  filters,
	  "javax.mail" % "mail" % "1.4.7",
	  "com.google.guava" % "guava" % "14.0.1",
	  "com.google.guava" % "guava-testlib" % "14.0.1" % "test",
	  "com.google.inject" % "guice" % "3.0",
	  "com.google.inject.extensions" % "guice-multibindings" % "3.0",
	  "org.elasticsearch" % "elasticsearch" % "0.90.5",
	  "org.elasticsearch" % "elasticsearch-cloud-aws" % "1.15.0",
	  "org.elasticsearch" % "elasticsearch-lang-javascript" % "1.4.0" exclude("log4j", "log4j"),
	  "org.jscience" % "jscience" % "4.3.1",
	  "org.jsoup" % "jsoup" % "1.7.2",
	  "net.sf.opencsv" % "opencsv" % "2.3",
	  "org.scribe" % "scribe" % "1.3.3",
	  "com.newrelic.agent.java" % "newrelic-api" % "2.21.3",
	  "org.seleniumhq.selenium" % "selenium-chrome-driver" % "2.32.0" % "test", // Must match Play's version of Selenium!
	  "org.apache.httpcomponents" % "httpcore" % "4.2.5" % "test",
	  "org.mockito" % "mockito-core" % "1.9.5" % "test",
	  "org.jvnet.mock-javamail" % "mock-javamail" % "1.9" % "test"
	)

	val defaultOptions = new CompilerOptions()
	CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(defaultOptions)
	defaultOptions.setProcessCommonJSModules(false)

	lazy val main = play.Project(appName, appVersion, appDependencies).settings(
    	closureCompilerSettings(defaultOptions) ++ Seq(
			resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
			lessEntryPoints <<= baseDirectory(_ / "app" / "assets" / "css" / "zeno.less"),
			javascriptEntryPoints <<= baseDirectory(_ / "app" / "assets" / "js" / "zeno.js")
		) : _*
	)
}
