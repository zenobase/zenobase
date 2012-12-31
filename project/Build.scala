import sbt._
import Keys._
import play.Project._
import com.google.javascript.jscomp.CompilerOptions
import com.google.javascript.jscomp.CompilationLevel

object ApplicationBuild extends Build {

    val appName         = "Zenobase"
    val appVersion      = "SNAPSHOT"

    val appDependencies = Seq(
      javaCore,
      "javax.mail" % "mail" % "1.4.5",
      "com.google.guava" % "guava" % "12.0.1",
      "com.google.guava" % "guava-testlib" % "12.0.1" % "test",
      "com.google.inject" % "guice" % "3.0",
      "com.google.inject.extensions" % "guice-multibindings" % "3.0",
      "org.elasticsearch" % "elasticsearch" % "0.20.2",
      "org.elasticsearch" % "elasticsearch-cloud-aws" % "1.10.0",
      "org.elasticsearch" % "elasticsearch-lang-javascript" % "1.2.0" exclude("log4j", "log4j"),
      "org.jscience" % "jscience" % "4.3.1",
      "org.jsoup" % "jsoup" % "1.7.1",
      "org.scribe" % "scribe" % "1.3.2",
      "newrelic.java-agent" % "newrelic-api" % "2.12.0",
      "org.seleniumhq.selenium" % "selenium-chrome-driver" % "2.28.0" % "test",
      "org.apache.httpcomponents" % "httpcore" % "4.2.3" % "test",
      "org.mockito" % "mockito-all" % "1.9.0" % "test",
      "org.jvnet.mock-javamail" % "mock-javamail" % "1.9" % "test"
    )

    val gzippableAssets = SettingKey[PathFinder]("gzippable-assets", "Defines the files to gzip")
    val gzipAssets = TaskKey[Seq[File]]("gzip-assets", "gzip all assets")
    lazy val gzipAssetsSetting = gzipAssets <<= gzipAssetsTask dependsOn (copyResources in Compile)
    lazy val gzipAssetsTask = (gzippableAssets, streams) map {
      case (finder: PathFinder, s: TaskStreams) => {
        var count = 0
        var files = finder.get.map { file =>
          val gzTarget = new File(file.getAbsolutePath + ".gz")
          IO.gzip(file, gzTarget)
          count += 1;
          gzTarget
        }
        s.log.info("Compressed " + count + " asset(s)")
        files
      }
    }

	val defaultOptions = new CompilerOptions()
	CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(defaultOptions)
	defaultOptions.setProcessCommonJSModules(false)

    val main = play.Project(appName, appVersion, appDependencies).settings(
      closureCompilerSettings(defaultOptions) ++ Seq(
        lessEntryPoints <<= baseDirectory(_ / "app" / "assets" / "css" / "zeno.less"),
        // requireJs += "main.js",
        javascriptEntryPoints <<= baseDirectory(_ / "app" / "assets" / "js" / "zeno.js"),
        resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
		resolvers += "New Relic" at "http://download.newrelic.com/",
        gzippableAssets <<= (classDirectory in Compile)(dir => (dir ** ("*.js" || "*.css" || "*.html"))),
        gzipAssetsSetting,
        playPackageEverything <<= playPackageEverything dependsOn gzipAssets
      ) : _*
    )
}
