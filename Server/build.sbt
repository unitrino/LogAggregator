name := "TJ"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= {
  val akkaV       = "2.3.14"
  val akkaStreamV = "2.0.1"
  Seq(
    "com.typesafe.akka" %% "akka-actor"                           % akkaV,
    "com.typesafe.akka" %% "akka-stream-experimental"             % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-core-experimental"          % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-experimental"               % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % akkaStreamV
  )
}

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.11"

libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41",
  "org.apache.commons" % "commons-dbcp2" % "2.0.1"
)



    