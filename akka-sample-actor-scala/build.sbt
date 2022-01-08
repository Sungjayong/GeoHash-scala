name := "PracticeProject"

version := "0.1"

scalaVersion := "2.13.7"

val AkkaVersion = "2.6.18"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.10" % "test"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.6.18" % Test
libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.32"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion

