package doobie

import cats.effect._
import cats.implicits._
import doobie.implicits._

object ConnectingToDatabase extends MyIOApp {

  def run(args: List[String]): IO[ExitCode] = {
    val q1 = transactor.use(42.pure[ConnectionIO].transact[IO]).unsafeRunSync()

    //run the query, interpret the resultset as a stream of Int values, and yield its one and only element.
    val q2 = transactor.use(sql"select 42".query[Int].unique.transact[IO]).unsafeRunSync()

    //compose two smaller programs
    val program = for {
      a <- sql"select 42".query[Int].unique
      b <- sql"select power(5, 2)".query[Int].unique
    } yield (a,b)
    val q3 = transactor.use(program.transact[IO]).unsafeRunSync()

    //Actually we need only an applicative functor in this case.
    val oneProgram = sql"select 42".query[Int].unique
    val anotherProgram = sql"select power(5, 2)".query[Int].unique
    val q4 = transactor.use((oneProgram, anotherProgram).mapN(_ + _).transact[IO]).unsafeRunSync()

    assert(q1 == 42 && q2 == 42 && q3 == (42, 25) && q4 == 67)
  }

}
