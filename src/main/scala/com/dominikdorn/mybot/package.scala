package com.dominikdorn

import zio.{ZEnv, ZIO}

package object mybot {

  type MyEffect[T] = ZIO[ZEnv, Throwable, T]

}
