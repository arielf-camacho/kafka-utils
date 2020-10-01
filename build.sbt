organization in ThisBuild := "com.github.arielf-camacho"
version in ThisBuild := "1.0.2"
scalaVersion in ThisBuild := "2.12.7"

val scalaOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xfuture",
  "-Xlint",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Yno-adapted-args",
  "-Ypartial-unification",
  "-Ywarn-unused-import"
)
scalacOptions ++= scalaOptions

val akkaVersion            = "2.5.22"
val akkaStreamKafkaVersion = "2.0.5"
val itDirectory            = "/src/integration-tests/scala"
val itAndTest              = "it,test"
val testContainer          = "1.12.0"

// Common options
val commonSettings = Seq(
  // Auto-format code configurations
  scalafmtOnCompile := true,
  scalafmtTestOnCompile := true,
  // Coverage configurations
  coverageMinimum := 60,
  coverageFailOnMinimum := true,
  coverageExcludedPackages := "<empty>;router;global;",
  coverageExcludedFiles := ".*BuildInfo.*;.*HealthCheckController.*;.*Routes.*;.*Application;.*Loader",
  //
  resolvers += Resolver.jcenterRepo,
  // Scala options
  scalacOptions ++= scalaOptions,
  // Scala doc
  autoAPIMappings := true,
  exportJars := true,
  scalacOptions in (Compile, doc) ++= Seq("-no-link-warnings"),
  scalacOptions in (Test, doc) ++= Opts.doc.externalAPI(
    (file(s"${(packageBin in Compile).value}") -> url("http://example.com/")) :: Nil
  )
)

lazy val `kafka-utils` = (project in file("."))
  .settings(commonSettings)
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      "com.dimafeng"       %% "testcontainers-scala"      % "0.38.4" % itAndTest,
      "com.typesafe"       % "config"                     % "1.3.2",
      "com.typesafe.akka"  %% "akka-actor"                % akkaVersion, // scalastyle:ignore
      "com.typesafe.akka"  %% "akka-stream"               % akkaVersion,
      "com.typesafe.akka"  %% "akka-stream-kafka"         % akkaStreamKafkaVersion,
      "com.typesafe.akka"  %% "akka-stream-kafka-testkit" % akkaStreamKafkaVersion,
      "com.typesafe.akka"  %% "akka-testkit"              % akkaVersion % itAndTest,
      "org.mockito"        % "mockito-core"               % "3.0.0" % itAndTest,
      "org.scalatest"      %% "scalatest"                 % "3.0.5" % itAndTest,
      "org.testcontainers" % "kafka"                      % testContainer % itAndTest
    ),
    scalacOptions ++= scalaOptions,
    scalaSource in IntegrationTest := baseDirectory.value / itDirectory
  )

useGpg := true
credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credential")
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/arielf-camacho/kafka-utils"),
    "scm:git@github.com:arielf-camacho/kafka-utils.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "arielf-camacho",
    name = "Your Name",
    email = "arielf.camacho@gmail.com",
    url = url("https://www.linkedin.com/in/ariel-felipe-camacho-d%C3%ADaz-ba3b21a2/")
  )
)
ThisBuild / description := "Custom utilities that simplify the use of Apacha Kafka."
ThisBuild / licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage := Some(url("https://github.com/arielf-camacho/kafka-utils"))
ThisBuild / pomIncludeRepository := { _ =>
  false
}
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true

testAll := (test in IntegrationTest).dependsOn(test in Test).value
lazy val testAll = TaskKey[Unit]("testAll")
