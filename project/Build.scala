import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "Zenobase"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      // "play" %% "play-test" % "2.0",
      "org.hamcrest" % "hamcrest-integration" % "1.2.1" withSources(),
      "com.google.guava" % "guava" % "11.0.1" withSources(),
      "com.google.inject" % "guice" % "3.0" withSources(),
      "org.elasticsearch" % "elasticsearch" % "0.19.0.RC3"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
      // Add your own project settings here      
    )

}
