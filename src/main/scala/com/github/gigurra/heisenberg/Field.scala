package com.github.gigurra.heisenberg

import com.github.gigurra.heisenberg.MapData.SourceData

import scala.reflect.runtime.universe.WeakTypeTag

sealed trait Field[+T] {
  def name: String
}

class FieldRequired[T: WeakTypeTag : MapDataParser : MapDataProducer](val name: String, default: => Option[T] = None) extends Field[T] {

  def mapDataParser: MapDataParser[T] = implicitly[MapDataParser[T]]

  def mapDataProducer: MapDataProducer[T] = implicitly[MapDataProducer[T]]

  def -->(t: T): (String, Any) = name -> MapDataProducer.produce(t)

  def parse(data: SourceData, orElse: => Option[T]): T = {
    data
      .get(name).map(MapDataParser.parse[T](_, name))
      .orElse(default)
      .orElse(orElse)
      .getOrElse(throw MapDataParser.MissingField(name = name, typ = implicitly[WeakTypeTag[T]].tpe.toString))
  }
  
  def getDefault: Option[T] = default

  def asOptional: FieldOption[T] = FieldOption[T](name)

  override def toString: String = s"$name: ${implicitly[WeakTypeTag[T]].tpe} <required>${default.fold("")(d => s" <default = $d>")}"
}

object FieldRequired {
  def apply[T: WeakTypeTag : MapDataParser : MapDataProducer](name: String, default: => Option[T] = None): FieldRequired[T] = new FieldRequired[T](name, default)
}

class FieldOption[T: WeakTypeTag : MapDataParser : MapDataProducer](val name: String, default: => Option[T] = None) extends Field[Option[T]] {

  def mapDataParser: MapDataParser[T] = implicitly[MapDataParser[T]]

  def mapDataProducer: MapDataProducer[T] = implicitly[MapDataProducer[T]]

  def -->(t: T): (String, Any) = name -> MapDataProducer.produce(t)

  def parse(data: SourceData, orElse: => Option[T]): Option[T] =
    data
      .get(name)
      .map {
        case Some(x) => x
        case None => null
        case null => null
        case x => x
      }
      .filter(_ != null)
      .map(MapDataParser.parse[T](_, name))
      .orElse(default)
      .orElse(orElse)

  def asOptional: FieldOption[T] = this

  def getDefault: Option[T] = default
  
  override def toString: String = s"$name: ${implicitly[WeakTypeTag[T]].tpe} <optional>"
}

object FieldOption {
  def apply[T: WeakTypeTag : MapDataParser : MapDataProducer](name: String, default: => Option[T] = None): FieldOption[T] = new FieldOption[T](name, default)
}

