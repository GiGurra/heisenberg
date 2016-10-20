package com.github.gigurra.heisenberg

trait FixErasure1[-T] {
}

trait FixErasure2[-T] {
}

object FixErasure1 {
  implicit val fix = new FixErasure1[Any] {}
}

object FixErasure2 {
  implicit val fix = new FixErasure2[Any] {}
}