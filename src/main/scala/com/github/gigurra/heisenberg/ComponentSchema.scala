package com.github.gigurra.heisenberg

import scala.reflect.runtime.universe.WeakTypeTag

trait ComponentSchema {
  protected def required[T: WeakTypeTag : MapDataParser : MapDataProducer](name: String, default: => T): FieldRequired[T]
  protected def required[T: WeakTypeTag : MapDataParser : MapDataProducer](name: String): FieldRequired[T]

  protected def optional[T: WeakTypeTag : MapDataParser : MapDataProducer](name: String, default: => Option[T]) : FieldOption[T]
  protected def optional[T: WeakTypeTag : MapDataParser : MapDataProducer](name: String) : FieldOption[T]
}
