
ThisBuild / organization := "org.u31"
ThisBuild / scalaVersion := "2.13.0-M5"
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .aggregate(server, database, tools)
  .dependsOn(server, database, tools)
  .settings(
    name := "Fastmail",

    libraryDependencies ++= Seq(
    )
  )

lazy val tools = (project in file("tools"))
  .settings(
    name := "Tools"
  )

lazy val database = (project in file("database"))
  .aggregate(tools)
  .dependsOn(tools)
  .settings(
    name := "Fastmail Database Sync",

    libraryDependencies ++= Seq(
      "com.orientechnologies" % "orientdb-client" % "3.0.18",
      "com.sun.mail" % "javax.mail" % "1.6.2",
    )
  )

lazy val server = (project in file("server"))
  .aggregate(tools)
  .dependsOn(tools)
  .settings(
    name := "Fastmail Websocket Server",

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"   % "10.1.8",
      "com.typesafe.akka" %% "akka-stream" % "2.5.19",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.8"
    )
  )
