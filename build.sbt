name := """mob"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, net.litola.SassPlugin)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "org.apache.opennlp" % "opennlp-tools" % "1.5.3",
  "com.typesafe.akka" %% "akka-testkit"  % "2.3.4" % "test"
)
