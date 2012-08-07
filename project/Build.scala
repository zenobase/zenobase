import scala.compat.Platform
import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "Zenobase"
    val appVersion      = "SNAPSHOT"

    val appDependencies = Seq(
      "javax.mail" % "mail" % "1.4.5",
      "com.google.guava" % "guava" % "12.0",
      "com.google.guava" % "guava-testlib" % "12.0" % "test",
      "com.google.inject" % "guice" % "3.0",
      "com.google.inject.extensions" % "guice-multibindings" % "3.0",
      "org.elasticsearch" % "elasticsearch" % "0.19.8",
      "org.elasticsearch" % "elasticsearch-cloud-aws" % "1.8.0",
      "org.seleniumhq.selenium" % "selenium-firefox-driver" % "2.25.0" % "test",
      "org.mockito" % "mockito-all" % "1.9.0" % "test",
      "org.jvnet.mock-javamail" % "mock-javamail" % "1.9" % "test"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
      lessEntryPoints <<= baseDirectory(_ / "app" / "assets" / "css" ** "zeno.less"),
      resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
    )

}
