import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5" % Test

  object doobie {
    lazy val core = "org.tpolecat" %% "doobie-core" % "0.8.8"
    lazy val h2Driver = "org.tpolecat" %% "doobie-h2" % "0.8.8"
    lazy val hikariCpTransactor = "org.tpolecat" %% "doobie-hikari" % "0.8.8"
    lazy val scalaTestSupport = "org.tpolecat" %% "doobie-scalatest" % "0.8.8" % Test
  }

}
