name := "sum"
import com.typesafe.sbt.packager.docker
import com.typesafe.sbt.packager.docker._

version := "0.1"
enablePlugins(JavaAppPackaging)
resolvers += Resolver.sonatypeRepo("releases")
scalaVersion := "2.12.8"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.1.7",
  "com.typesafe.akka" %% "akka-stream" % "2.5.19",
  "commons-io" % "commons-io" % "2.6",
"org.apache.httpcomponents" % "httpmime" % "4.5.7",
"org.apache.httpcomponents" % "httpclient" % "4.5.7",
"com.typesafe.akka" %% "akka-http-spray-json" % "10.1.8",
  "com.github.hayasshi" %% "akka-http-router" % "0.4.0",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.9",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.9"
)

daemonUserUid in Docker := None
daemonUser in Docker := "root"
dockerExposedPorts := Seq(9092)
dockerAlias := docker.DockerAlias(None,Option("tiff16"),"master-project",Option("second_scala_akka_sum"))