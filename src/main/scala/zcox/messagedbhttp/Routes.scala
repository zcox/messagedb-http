package zcox.messagedbhttp

import cats.syntax.all._
import cats.effect._
// import cats.effect.kernel.Resource
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.dsl._
import org.http4s.circe._
import messagedb._
// import fs2.Stream
import java.util.UUID
import scala.concurrent.duration._

object Routes {

  case class PutMessageRequest(
    `type`: String,
    data: Json,
    metadata: Option[Json],
    expectedVersion: Option[Long],
  ) {
    def toMessage(id: UUID, streamName: String): MessageDb.Write.Message = 
      MessageDb.Write.Message(
        id = id, 
        streamName = streamName,
        `type` = this.`type`,
        data = this.data,
        metadata = this.metadata,
        expectedVersion = this.expectedVersion,
      )
  }

  object PutMessageRequest {
    implicit val decoder: Decoder[PutMessageRequest] = deriveDecoder
    implicit def entityDecoder[F[_]: Concurrent] = jsonOf[F, PutMessageRequest]
  }

  case class PutMessageResponse(
    position: Long,
  )

  object PutMessageResponse {
    implicit val encoder: Encoder[PutMessageResponse] = deriveEncoder
  }

  implicit val messageEncoder: Encoder[MessageDb.Read.Message] = deriveEncoder

  /** messagedb allows you to write to a stream name without a '-' but if you try to read 
   * from a stream name without a '-' it throws an error. */
  object StreamNameVar {
    def unapply(s: String): Option[String] = 
      if (s.contains('-'))
        s.some
      else
        none
  }

  def routes[F[_]: Temporal](
    // messageDbPool: Resource[F, MessageDb[F]]
    messageDb: MessageDb[F]
  ): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {

      // case GET -> Root / "streams" / streamName / "messages" =>
      //   Ok(
      //     Stream
      //       .resource[F, MessageDb[F]](messageDbPool)
      //       .flatMap(_.getStreamMessages(streamName, None, None, None).map(_.asJson))
      //   )

      case GET -> Root / "streams" / StreamNameVar(streamName) / "messages" =>
        Ok(messageDb.getStreamMessages(streamName, None, None, None).map(_.asJson))

      case GET -> Root / "streams" / StreamNameVar(streamName) / "messages" / "first" =>
        Ok(messageDb.getFirstStreamMessage(streamName).map(_.asJson))

      case GET -> Root / "streams" / StreamNameVar(streamName) / "messages" / "last" =>
        Ok(messageDb.getLastStreamMessage(streamName).map(_.asJson))

      case GET -> Root / "categories" / category / "messages" =>
        Ok(messageDb.getCategoryMessages(category, None, None, None, None, None, None).map(_.asJson))

      case GET -> Root / "categories" / category / "messages" / "unbounded" => 
        Ok(
          messageDb
            .getCategoryMessagesUnbounded(category, 0, None, None, None, None, None, 1.second)
            .map(_.asJson.noSpaces + "\n")
        )

      case req @ PUT -> Root / "streams" / StreamNameVar(streamName) / "messages" / UUIDVar(eventId) =>
        for {
          body <- req.as[PutMessageRequest]
          //TODO handle errors
          position <- messageDb.writeMessage(body.toMessage(eventId, streamName))
          resp <- Ok(PutMessageResponse(position).asJson)
        } yield resp

    }
  }
}
