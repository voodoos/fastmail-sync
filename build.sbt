scalaVersion := "2.13.0-M5"
organization := "org.u31"
name := "Fastmail"

libraryDependencies ++= Seq(
  "com.orientechnologies" % "orientdb-client" % "3.0.18",
  "com.sun.mail" % "javax.mail" % "1.6.2",
  "com.typesafe.akka" %% "akka-http"   % "10.1.8",
  "com.typesafe.akka" %% "akka-stream" % "2.5.19"
)