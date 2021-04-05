package com.dominikdorn.mybot

import com.bot4s.telegram.clients.SttpClient
import com.bot4s.telegram.marshalling
import com.bot4s.telegram.marshalling.CaseConversions
import com.bot4s.telegram.methods.{JsonRequest, MultipartRequest, Request, Response}
import com.bot4s.telegram.models.InputFile
import com.softwaremill.sttp.SttpBackend
import io.circe.{Decoder, Encoder}
import io.circe.parser.parse
import zio.ZIO

import scala.util.Try

// patched client that should be able to handle failures in response processing of processApiResponse
class PatchedSttpClient(val token: String)(implicit val backend: SttpBackend[MyEffect, Nothing],
                                           monadError: cats.MonadError[MyEffect, Throwable]) extends SttpClient[MyEffect](token, "api.telegram.org"){
  import com.softwaremill.sttp.{Request => _, Response => _, _}
  private implicit def circeBodySerializer[B : Encoder]: BodySerializer[B] =
    b => StringBody(marshalling.toJson[B](b), "utf-8", Some(MediaTypes.Json))

  private val apiBaseUrl = s"https://api.telegram.org/bot$token/"

  private def asJson[B : Decoder]: ResponseAs[B, Nothing] =
    asString("utf-8").map(s => marshalling.fromJson[B](s))


  def sendRequest2[R, T <: Request[_]](request: T)(implicit encT: Encoder[T], decR: Decoder[R]): MyEffect[R] = {
    val url = apiBaseUrl + request.methodName

    val sttpRequest: RequestT[Id, String, Nothing] = request match {
      case r: JsonRequest[_] =>
        sttp.post(uri"$url").body(request)

      case r: MultipartRequest[_] =>
        val files = r.getFiles

        val parts = files.map {
          case (camelKey, inputFile) =>
            val key = CaseConversions.snakenize(camelKey)
            inputFile match {
              case InputFile.FileId(id) => multipart(key, id)
              case InputFile.Contents(filename, contents) => multipart(key, contents).fileName(filename)
              //case InputFile.Path(path) => multipartFile(key, path)
              case other =>
                throw new RuntimeException(s"InputFile $other not supported")
            }
        }

        val fields = parse(marshalling.toJson(request)).fold(throw _, _.asObject.map {
          _.toMap.mapValues {
            json =>
              json.asString.getOrElse(marshalling.printer.pretty(json))
          }
        })

        val params = fields.getOrElse(Map())

        sttp.post(uri"$url?$params").multipartBody(parts)
    }

    import com.bot4s.telegram.marshalling.responseDecoder

    val response = sttpRequest
      .readTimeout(readTimeout)
      .parseResponseIf(_ => true) // Always parse response
      .response(asJson[Response[R]])
      .send[MyEffect]()

    response
      .map(_.unsafeBody)
      .flatMap(t => monadError.fromTry(Try(processApiResponse[R](t))))
  }

  override def sendRequest[R, T <: Request[_]](request: T)(implicit encT: Encoder[T], decR: Decoder[R]): MyEffect[R] = for {
    _ <- zio.console.putStrLn(s"sending request: ${request}")
    ef <- sendRequest2(request)
  } yield ef
}
