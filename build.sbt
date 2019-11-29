organization in ThisBuild := "com.github.arielf-camacho"
version in ThisBuild := "1.0.0"
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
val akkaStreamKafkaVersion = "1.0.4"
val itDirectory            = "/src/integration-tests/scala"

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
      "com.dimafeng"      %% "testcontainers-scala"      % "0.31.0" withSources,
      "com.typesafe"      % "config"                     % "1.3.2" withSources,
      "com.typesafe.akka" %% "akka-actor"                % akkaVersion withSources, // scalastyle:ignore
      "com.typesafe.akka" %% "akka-stream"               % akkaVersion withSources,
      "com.typesafe.akka" %% "akka-stream-kafka"         % akkaStreamKafkaVersion withSources,
      "com.typesafe.akka" %% "akka-stream-kafka-testkit" % akkaStreamKafkaVersion withSources,
      "com.typesafe.akka" %% "akka-testkit"              % akkaVersion % "it,test" withSources,
      "org.mockito"       % "mockito-core"               % "3.0.0" withSources,
      "org.scalatest"     %% "scalatest"                 % "3.0.5" withSources
    ),
    scalacOptions ++= scalaOptions,
    scalaSource in IntegrationTest := baseDirectory.value / itDirectory
  )

useGpg := true
credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credential")
ThisBuild / pomIncludeRepository := { _ =>
  false
}
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true
