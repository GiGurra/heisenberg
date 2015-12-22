package se.gigurra.heisenberg

import se.gigurra.heisenberg.MapData.SourceData

import scala.reflect.runtime.universe.TypeTag

sealed trait Field[+T] {
  def name: String
}

class FieldRequired[T: TypeTag : MapDataParser : MapDataProducer](val name: String, default: => Option[T] = None) extends Field[T] {

  def -->(t: T): (String, Any) = name -> MapDataProducer.produce(t)

  def parse(data: SourceData): T = {
    data
      .get(name).map(MapDataParser.parse[T](_, name))
      .orElse(default)
      .getOrElse(throw MapDataParser.MissingField(name = name, typ = implicitly[TypeTag[T]].tpe.toString))
  }

  def asOptional: FieldOption[T] = FieldOption[T](name)

  override def toString: String = s"$name: ${implicitly[TypeTag[T]].tpe} <required>${default.fold("")(d => s" <default = $d>")}"
}

object FieldRequired {
  def apply[T: TypeTag : MapDataParser : MapDataProducer](name: String, default: => Option[T] = None): FieldRequired[T] = new FieldRequired[T](name, default)
}

class FieldOption[T: TypeTag : MapDataParser : MapDataProducer](val name: String) extends Field[Option[T]] {
  def -->(t: T): (String, Any) = name -> MapDataProducer.produce(t)
  def parse(data: SourceData): Option[T] = data.get(name).map(MapDataParser.parse[T](_, name))

  def asOptional: FieldOption[T] = this

  override def toString: String = s"$name: ${implicitly[TypeTag[T]].tpe} <optional>"
}

object FieldOption {
  def apply[T: TypeTag : MapDataParser : MapDataProducer](name: String): FieldOption[T] = new FieldOption[T](name)
}

