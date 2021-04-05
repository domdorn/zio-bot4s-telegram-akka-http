package com.dominikdorn.mybot

import com.bot4s.telegram.api.declarative.{Commands, Messages}
import com.bot4s.telegram.cats.{Polling, TelegramBot}
import com.bot4s.telegram.clients.SttpClient
import com.bot4s.telegram.methods.{ExportChatInviteLink, GetMe}
import com.bot4s.telegram.models.{Update, User}
import com.softwaremill.sttp.SttpBackend
import zio.ZIO

class MyBot(token: String)(implicit val backend: SttpBackend[MyEffect, Nothing],
                               catsMonadError: cats.MonadError[MyEffect, Throwable]) extends TelegramBot[MyEffect](token, backend)
  with Polling[MyEffect]
  with Commands[MyEffect]
  with Messages[MyEffect] {

  override val client: SttpClient[MyEffect] = new PatchedSttpClient(token)

  override def receiveUpdate(u: Update, botUser: Option[User]): MyEffect[Unit] = {
    zio.console.putStrLn(s"received update ${u} from botUser ${botUser}") *>
    super.receiveUpdate(u.copy(message = u.message.map(m =>
      m.migrateToChatId.map(l => m.copy(chat = m.chat.copy(l))).getOrElse(m)
    )), botUser)
  }

//  onMessage { implicit msg =>
//    (for {
//      _ <- ZIO.unit
//      t <- ZIO.fromOption(msg.text)
//      r <- reply(s"thx for $t")(msg)
//    } yield ()).ignore
//  }

  onCommand("help") { implicit msg =>
    withArgs{
      case Seq("default") => reply("here is your default help").unit
      case Seq("getMe") => request(GetMe).flatMap(r => reply(s"received ${r}")).ignore
      case Seq("invite") => (request(ExportChatInviteLink(msg.chat.id)).flatMap(l => reply(s"here you go: ${l}")))
        .catchAll(e => zio.console.putStrLn("got error: " + e) *> reply(s"unfortunately, I cant do that.. got an exception ${e}").unit).unit
      case s: Seq[_] =>
        reply("hier ist deine hilfe" + msg.contact).unit *>
          reply("und das war die nachricht " + msg.toString).unit
      ZIO.unit
    }
  }

  onEditedMessage(
    implicit msg =>
      reply("hey, I've seen that you edited your message!").unit
  )

  onExtMessage {
    case (m, Some(botUser)) =>
      zio.console.putStrLn(s"received message for ${botUser}: $m") *>
      reply(s"hey! I'm ${botUser.id}, you're ${m.from} i got ya")(m).ignore
    case (m, None) =>
      zio.console.putStrLn("received message without user") *>
      reply(s"strange, don't know who you are")(m).ignore
  }

  onMessage { implicit msg =>
    for {
      _ <- zio.console.putStrLn("in onMessage")
      _ <- ZIO.fromOption(msg.text).flatMap(t => for {
        _ <- zio.console.putStrLn("in msg.text")
        r <- reply(s"Hallo! Schön, dass du mir ${t} geschickt hast!")(msg)
        _ <- zio.console.putStrLn("replied to a message")
      } yield ()).ignore
      _ <- ZIO.fromOption(msg.replyToMessage).flatMap(t => for {
        _ <- zio.console.putStrLn("in msg.replyToMessage")
        r <- reply(s"Hallo! Schön, dass du mir die antwort ${t} geschickt hast!")(msg)
        _ <- zio.console.putStrLn("replied to a message")
      } yield ()).ignore
    } yield ()

  }

}
