import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "Zenobase"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "org.hamcrest" % "hamcrest-integration" % "1.2.1",
      "com.google.guava" % "guava" % "11.0.1",
      "com.google.inject" % "guice" % "3.0",
      "org.elasticsearch" % "elasticsearch" % "0.19.0"
      // "org.jscience" % "jscience" % "4.3.1"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
      // Add your own project settings here      
    )

}
