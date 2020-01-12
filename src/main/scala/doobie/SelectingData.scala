package doobie

import cats.data.NonEmptyList
import cats.effect.{ExitCode, IO}
import cats.implicits._
import doobie.implicits._

object SelectingData extends MyIOApp {

  // The sql string interpolator allows us to create a query to select data from the database.

  override def run(args: List[String]): IO[ExitCode] = {
    insertData().unsafeRunSync()

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

}
