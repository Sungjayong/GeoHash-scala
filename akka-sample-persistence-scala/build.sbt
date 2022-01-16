name := "akka-sample-persistence-scala"

version := "0.1"

scalaVersion := "2.13.8"

val AkkaVersion = "2.6.18"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-persistence-cassandra" % "1.0.5",
  "com.typesafe.akka" %% "akka-persistence" % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence-query" % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.9" % Test,
  "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "3.0.4",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "com.lightbend.akka" %% "akka-projection-cassandra" % AkkaVersion,
)