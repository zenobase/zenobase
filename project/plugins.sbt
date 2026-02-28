logLevel := Level.Warn

updateOptions := updateOptions.value.withCachedResolution(true)

evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)

resolvers ++= Seq(
  Resolver.defaultLocal,
  "Maven Central" at "https://repo.maven.apache.org/maven2/",
  "Lightbend Maven Releases" at "https://dl.cloudsmith.io/public/lightbend/maven-releases/maven/"
)

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.10")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.6")

addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-uglify" % "1.0.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")

addSbtPlugin("com.slidingautonomy.sbt" % "sbt-filter" % "1.0.1")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")
