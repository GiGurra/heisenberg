package se.gigurra.heisenberg

import MapData.SourceData

import scala.language.implicitConversions
import scala.reflect.runtime.universe.WeakTypeTag

abstract class Schema[T <: Parsed[_] : WeakTypeTag] {

  implicit val instance: this.type = this

  //////////////////////////////////////////
  // Definition API

  protected def required[F: WeakTypeTag : MapDataParser : MapDataProducer](name: String, default: => F): FieldRequired[F] = addField[F, FieldRequired[F]](FieldRequired(name, Some(default)))
  protected def required[F: WeakTypeTag : MapDataParser : MapDataProducer](name: String): FieldRequired[F] = addField[F, FieldRequired[F]](FieldRequired(name, None))

  protected def optional[F: WeakTypeTag : MapDataParser : MapDataProducer](name: String, default: => Option[F]): FieldOption[F] = addField[Option[F], FieldOption[F]](FieldOption(name, default))
  protected def optional[F: WeakTypeTag : MapDataParser : MapDataProducer](name: String): FieldOption[F] = addField[Option[F], FieldOption[F]](FieldOption(name, None))


  //////////////////////////////////////////
  // API

  def fields: Set[Field[Any]] = _fields

  def apply(objectData: SourceData): T

  def marshal(fields: MapData*): T = MapParser.parse[T](MapData(fields: _*))

  val defaultParser: MapParser[T] = Parsed.Parser[T](Schema.this.apply)

  implicit def parser: MapParser[T] = defaultParser

  override def toString: String = s"Schema (${getClass.getName}):\n\t${fields.mkString("\n\t")}"

  implicit def dynamic2parsed(dd: MapData): T = marshal(dd)

  def parse(sourceData: SourceData): T = MapParser.parse[T](sourceData)

  //////////////////////////////////////////
  // Helpers

  private var _fields: Set[Field[Any]] = Set.empty

  private def addField[F, FieldType <: Field[F]](f: FieldType): FieldType = {
    _fields += f
    f
  }

}
