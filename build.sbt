

lazy val root = project
  .in(file("."))
  .settings(
    organization := "com.dominikdorn.bot4s",
    name := "zio-telegram-akka-http-sample",
    scalaVersion := "2.13.5",
    // Core with minimal dependencies, enough to spawn your first bot.
    libraryDependencies += "com.bot4s" %% "telegram-core" % "5.0.0-SNAPSHOT",
    libraryDependencies ++= Dependencies.akka,
    libraryDependencies ++= Dependencies.sttp,
    libraryDependencies ++= Dependencies.effects,
  )

resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
