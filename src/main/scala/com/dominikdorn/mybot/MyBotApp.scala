package com.dominikdorn.mybot

import akka.actor.ActorSystem
import com.dominikdorn.mybot.ZIOSttp._
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import zio.{ExitCode, URIO, ZIO, ZManaged}

object MyBotApp extends zio.App {

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val actorSystemManaged = ZManaged.make(ZIO.effect(ActorSystem()))(a =>
      zio.console.putStrLn("shutting down akka") *> ZIO.fromFuture(ec => a.terminate()).orDie.unit)
    val actorSystemLayer = actorSystemManaged.toLayer

    val sttpAkkaBackend = for {
      akka <- ZManaged.service[ActorSystem]
      futureBackend <- ZManaged.make(ZIO.succeed(AkkaHttpBackend.usingActorSystem(akka)))(b => ZIO.effect(b.close()).orDie)
    } yield futureBackend

    val wrappedBackend = sttpAkkaBackend.map(s => ZIOSttpBackendWrapper[Nothing](s):SttpBackend[MyEffect, Nothing])

    val ef = for {
      akka <- ZManaged.service[ActorSystem]
      wrappedBackend <- ZManaged.service[SttpBackend[MyEffect, Nothing]]

      myBot <- ZManaged.make(ZIO.effect(new MyBot(akka.settings.config.getString("mybot.token"))(wrappedBackend, ZIOSttp.myEffectMonadError[Throwable]())))(b => zio.console.putStrLn("shutting down bot") *> ZIO.effect(b.shutdown()).orDie)
      _ <- myBot.run().forkDaemon.toManaged_
      _ <- zio.console.putStrLn("Everything started, bot is ready").toManaged_
      _ <- zio.console.putStrLn("Press [ENTER] to shutdown the bot, it may take a few seconds...").toManaged_
      _ <- zio.console.getStrLn.toManaged_
    } yield ()

    val e = ef.useNow.onInterrupt(zio.console.putStrLn("interrupt received"))

    val resolvedBackend = actorSystemLayer >>> wrappedBackend.toLayer
    val E = actorSystemLayer ++ resolvedBackend

    val layers = zio.ZEnv.live >>> E

    e.provideCustomLayer(layers).exitCode
  }

}
