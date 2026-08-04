scalaVersion := "3.8.4"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "dv01_challenge",
    libraryDependencies ++= Seq(
      guice,
      "org.playframework" %% "play-json" % "3.0.5",
      "org.scalameta" %% "munit" % "1.0.4" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
