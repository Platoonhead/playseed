name := """hills-brows-cappuccino"""
organization := "3tierlogic"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.11"

javaOptions in Test += "-Dconfig.file=conf/test.conf"

coverageExcludedPackages := "<empty>;Reverse.*;router.Routes.*;controllers.JavascriptRoutes.*;views\\..*"

libraryDependencies ++= Seq(
  ehcache,
  ws,
  filters,
  guice,
  "net.codingwell" %% "scala-guice" % "4.1.0",
  "com.typesafe.play" % "play-slick_2.11" % "3.0.0",
  "com.typesafe.play" % "play-slick-evolutions_2.11" % "3.0.0",
  "mysql" % "mysql-connector-java" % "5.1.35",
  "com.github.tototoshi" % "slick-joda-mapper_2.11" % "2.3.0",
  "org.joda" % "joda-convert" % "1.8.1",
  "com.sendgrid" % "sendgrid-java" % "3.1.0",
  "com.h2database" % "h2" % "1.4.196",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "com.cloudphysics" % "jerkson_2.10" % "0.6.3",
  "com.typesafe.play" % "play-json-joda_2.11" % "2.6.0-RC1",
  "org.scalatestplus.play" % "scalatestplus-play_2.11" % "3.1.1" % Test,
  "com.typesafe.akka" % "akka-testkit_2.11" % "2.4.17",
  specs2 % Test,
  evolutions
)


// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
