package zcox.messagedbhttp

import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.comcast.ip4s._
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger
import skunk._
import natchez.Trace.Implicits.noop
import cats.effect.std.Console
import messagedb._

object Server {

  def stream[F[_]: Async: Console]: Stream[F, Nothing] = {
    for {
      sessionPool <- Stream.resource(
        Session.pooled(
          host = "localhost",
          port = 5432,
          user = "postgres",
          database = "message_store",
          password = Some("postgres"),
          max = 10,
          parameters = Map(
            //messagedb's tables etc are in the message_store schema, not public schema
            "search_path" -> "message_store", 
            //http://docs.eventide-project.org/user-guide/message-db/server-functions.html#filtering-messages-with-a-sql-condition
            "message_store.sql_condition" -> "on"
          ) ++ Session.DefaultConnectionParameters,
        )
      )
      // messageDbPool = MessageDb.fromPool1[F](sessionPool)
      messageDb = MessageDb.fromPool2[F](sessionPool)

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract segments not checked
      // in the underlying routes.
      httpApp = (
        Routes.routes[F](messageDb)
      ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      exitCode <- Stream.resource(
        EmberServerBuilder.default[F]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(finalHttpApp)
          .build >>
        Resource.eval(Async[F].never)
      )
    } yield exitCode
  }.drain
}
