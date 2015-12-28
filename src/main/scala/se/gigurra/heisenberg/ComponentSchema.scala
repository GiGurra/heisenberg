package se.gigurra.heisenberg

import scala.reflect.runtime.universe.WeakTypeTag

trait ComponentSchema {
  protected def required[T: WeakTypeTag : MapDataParser : MapDataProducer](name: String, default: => T = null.asInstanceOf[T]): FieldRequired[T]
  protected def optional[T: WeakTypeTag : MapDataParser : MapDataProducer](name: String, default: => Option[T] = None) : FieldOption[T]
}
