lazy val akkaHttpVersion = "10.2.6"
lazy val akkaVersion    = "2.6.17"
lazy val leveldbVersion = "0.12"
lazy val leveldbjniVersion = "1.8"
val AkkaManagementVersion = "1.1.1"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "br.usp",
      scalaVersion    := "2.13.4"
    )),
    name := "consumer-service",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-typed"   % akkaVersion,
      "ch.qos.logback"    % "logback-classic"           % "1.2.6",

      "com.typesafe.akka" %% "akka-stream-kafka" % "2.1.1",

      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,

      "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,

      "com.lightbend.akka.management" %% "akka-management" % AkkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion,

      "org.mongodb.scala" %% "mongo-scala-driver" % "4.0.6",
      "com.github.scullxbones" %% "akka-persistence-mongo-scala" % "3.0.6",
      "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.2.9"         % Test
    )
  )