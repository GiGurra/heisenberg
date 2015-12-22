package se.gigurra.heisenberg

import scala.reflect.runtime.universe.TypeTag

trait ComponentSchema {
  protected def required[T: TypeTag : MapDataParser : MapDataProducer](name: String, default: => T = null.asInstanceOf[T]): FieldRequired[T]
  protected def optional[T: TypeTag : MapDataParser : MapDataProducer](name: String) : FieldOption[T]
}
