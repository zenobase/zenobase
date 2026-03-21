logLevel := Level.Warn

updateOptions := updateOptions.value.withCachedResolution(true)

evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)

resolvers ++= Seq(
  Resolver.defaultLocal,
  "Maven Central" at "https://repo.maven.apache.org/maven2/",
  "Lightbend Maven Releases" at "https://dl.cloudsmith.io/public/lightbend/maven-releases/maven/"
)

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.10")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")
