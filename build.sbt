scalaVersion := "3.8.4"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "dv01_challenge",
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http" % "1.3.0",
      "org.scalameta" %% "munit" % "1.0.4" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
