package doobie

import cats.data.NonEmptyList
import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource}
import doobie.hikari.HikariTransactor
import cats.implicits._
import doobie.implicits._

trait MyIOApp extends IOApp {

  protected val transactor: Resource[IO, HikariTransactor[IO]] = for {
    ce <- ExecutionContexts.fixedThreadPool[IO](32) //connect execution context
    be <- Blocker[IO] //blocking execution context
    xa <- HikariTransactor.newHikariTransactor[IO](
      driverClassName = "org.h2.Driver",
      url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
      user = "sa",
      pass = "",
      ce, // await connection here
      be // execute JDBC operations here
    )
  } yield xa

  protected def assert(f: => Boolean): IO[ExitCode] = {
    IO.pure(if (f) ExitCode.Success else ExitCode.Error)
  }

  protected def insertData(): IO[Unit] = {
    val countries = List(
      Country("DEU", "Germany", 82164700, Some(2133367.00)),
      Country("ESP", "Spain", 39441701, None),
      Country("FRA", "France", 59225700, Some(1424285.00)),
      Country("GBR", "United Kingdom", 59623400, Some(1378332.00)),
      Country("USA", "United States of America", 278357000, Some(8510700.00))
    ).toNel.get

    transactor.use((createCountryTable() *> insertCountries(countries)).transact(_))
  }

  case class Country(
                      code: String,
                      name: String,
                      population: Int,
                      gnp: Option[Double]
                    )

  private def createCountryTable(): ConnectionIO[Unit] = {
    sql"""
         |CREATE TABLE country(
         |  code character (3) NOT NULL,
         |  name text NOT NULL,
         |  population integer NOT NULL,
         |  gnp numeric (10, 2)
         |);
         |""".stripMargin
      .update.run.void
  }

  private def insertCountries(nel: NonEmptyList[Country]): ConnectionIO[Unit] = {
    val sql = "INSERT INTO country (code, name, population, gnp) VALUES (?, ?, ?, ?)"
    Update[Country](sql).updateMany(nel).void
  }

}
