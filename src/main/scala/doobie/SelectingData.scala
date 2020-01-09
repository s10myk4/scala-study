package doobie

import cats.data.NonEmptyList
import cats.effect.{ExitCode, IO}
import cats.effect._
import cats.implicits._
import doobie.implicits._
import doobie._

object SelectingData extends MyIOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val countries = List(
      Country("DEU", "Germany", 82164700, 2133367.00),
      Country("ESP", "Spain", 39441701, null),
      Country("FRA", "France", 59225700, 1424285.00),
      Country("GBR", "United Kingdom", 59623400, 1378330.00),
      Country("USA", "United States of America", 278357000, 8510700.00)
    ).toNel.get

    val countryNameQuery = sql"select name from COUNTRY where code = 'ESP'".query[String].unique

    val res = transactor.use(
      (createCountryTable() *> insertCountries(countries) *> countryNameQuery).transact[IO]
    ).unsafeRunSync()

    asset(res == "Spain")
  }

  case class Country(
                      code: String,
                      name: String,
                      population: Int,
                      gnp: Double
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
