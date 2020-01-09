package doobie

import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource}
import doobie.hikari.HikariTransactor

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

  protected def asset(f: => Boolean): IO[ExitCode] = {
    IO.pure(if (f) ExitCode.Success else ExitCode.Error)
  }

}
