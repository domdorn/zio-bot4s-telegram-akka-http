package com.dominikdorn.mybot

import cats.{MonadError => CMonadError}
import com.softwaremill.sttp.{
  Request,
  Response,
  SttpBackend,
  MonadAsyncError => SttpMonadAsyncError,
  MonadError => SttpMonadError
}
import zio.{Task, ZIO}

import scala.concurrent.Future

object ZIOSttp {

  case class ZIOSttpBackendWrapper[S](underlying: SttpBackend[Future, S]) extends SttpBackend[MyEffect, S] {
    override def send[T](request: Request[T, S]): MyEffect[Response[T]] = ZIO.fromFuture(ec => underlying.send(request))
    override def close(): Unit = underlying.close()
    override def responseMonad: SttpMonadError[MyEffect] = sttpMyEffectMonadError
  }


  implicit val sttpMyEffectMonadError: SttpMonadError[MyEffect] = new SttpMonadAsyncError[MyEffect] {
    override def async[T](register: (Either[Throwable, T] => Unit) => Unit): Task[T] = for {
        p <- zio.Promise.make[Throwable, T]
        _ = register {
          case Left(t) => p.fail(t)
          case Right(v) => p.succeed(v)
        }
        v <- p.await
      } yield v

    override def unit[T](t: T): MyEffect[T] = Task.effect(t)
    override def map[T, T2](fa: MyEffect[T])(f: T => T2): MyEffect[T2] = fa.map(f)
    override def flatMap[T, T2](fa: MyEffect[T])(f: T => MyEffect[T2]): MyEffect[T2] = fa.flatMap(f)
    override def error[T](t: Throwable): MyEffect[T] = ZIO.fail(t)
    override protected def handleWrappedError[T](rt: MyEffect[T])(h: PartialFunction[Throwable, MyEffect[T]]): MyEffect[T] = rt.catchAll(h)
  }


  def wrappedMonadError[E](): CMonadError[MyEffect, E] = zio.interop.catz.monadErrorInstance.asInstanceOf[CMonadError[MyEffect, E]]
  implicit def myEffectMonadError[E](): CMonadError[MyEffect, E] = new CMonadError[MyEffect, E] {
    override def raiseError[A](e: E): MyEffect[A] = wrappedMonadError().raiseError(e)

    override def handleErrorWith[A](fa: MyEffect[A])(f: E => MyEffect[A]): MyEffect[A] = wrappedMonadError().handleErrorWith(fa)(f)

    override def pure[A](x: A): MyEffect[A] = ZIO.effect(x)

    override def flatMap[A, B](fa: MyEffect[A])(f: A => MyEffect[B]): MyEffect[B] = wrappedMonadError().flatMap(fa)(f)

    override def tailRecM[A, B](a: A)(f: A => MyEffect[Either[A, B]]): MyEffect[B] = wrappedMonadError().tailRecM(a)(f)
  }

}
