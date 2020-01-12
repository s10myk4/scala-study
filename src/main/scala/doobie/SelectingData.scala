package doobie

import cats.data.NonEmptyList
import cats.effect.{ExitCode, IO}
import cats.implicits._
import doobie.implicits._

object SelectingData extends MyIOApp {

  // The sql string interpolator allows us to create a query to select data from the database.

  override def run(args: List[String]): IO[ExitCode] = {
    val countries = List(
      Country("DEU", "Germany", 82164700, Some(2133367.00)),
      Country("ESP", "Spain", 39441701, None),
      Country("FRA", "France", 59225700, Some(1424285.00)),
      Country("GBR", "United Kingdom", 59623400, Some(1378332.00)),
      Country("USA", "United States of America", 278357000, Some(8510700.00))
    ).toNel.get

    transactor.use((createCountryTable() *> insertCountries(countries)).transact(_)).unsafeRunSync()

    //unique: if we expect the query to return only one row
    val countryName = transactor.use(
      sql"select name from country where code = 'ESP'".query[String].unique.transact(_)
    ).unsafeRunSync()

    //option: if we are not sure if the record exists
    val maybeCountryName = transactor.use(
      sql"select name from country where code = 'ITA'".query[String].option.transact(_)
    ).unsafeRunSync()

    //accumulate the results in a List
    val countryNames = transactor.use(
      sql"select name from country order by name".query[String].to[List].transact(_)
    ).unsafeRunSync()

    //stream.take(n): By applying take(3) we instruct the stream to shut everything down (and clean everything up) after five elements have been emitted
    val limitedCountryNames = transactor.use(
      sql"select name from country order by name".query[String].stream.take(3).compile.toList.transact(_)
    ).unsafeRunSync()

    assert {
      countryName == "Spain" &&
      maybeCountryName.isEmpty &&
      countryNames == List("France", "Germany", "Spain", "United Kingdom", "United States of America") &&
      limitedCountryNames.length == 3
    }
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
