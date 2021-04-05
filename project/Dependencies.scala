import sbt._
import sbt.librarymanagement.ModuleID

object Dependencies {

  object Version {
    val akkaVersion = "2.6.13"
    val akkaHttp = "10.2.4"
    val sttpVersion = "1.7.2"
  }

  val akka: Seq[ModuleID] = Seq(
        "com.typesafe.akka" %% "akka-http" % Version.akkaHttp,
        "com.typesafe.akka" %% "akka-http-testkit" % Version.akkaHttp % Test,
        "com.typesafe.akka" %% "akka-actor" % Version.akkaVersion,
        "com.typesafe.akka" %% "akka-stream" % Version.akkaVersion,
        "com.softwaremill.sttp" %% "core" % Version.sttpVersion,
        "com.softwaremill.sttp" %% "akka-http-backend" % Version.sttpVersion
  )
  val sttp: Seq[ModuleID] = Seq(
        "com.softwaremill.sttp" %% "core" % Version.sttpVersion,
        "com.softwaremill.sttp" %% "akka-http-backend" % Version.sttpVersion
    //    libraryDependencies += "com.softwaremill.sttp.client3" %% "httpclient-backend-zio" % "3.2.3",
  )


  val effects: Seq[ModuleID] = Seq(
    "org.typelevel" %% "cats-effect" % "2.4.0",
    "dev.zio" %% "zio" % "1.0.5",
    "dev.zio" %% "zio-interop-cats" % "2.4.0.0"
  )

}
