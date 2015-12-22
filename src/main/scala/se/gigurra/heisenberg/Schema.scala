package se.gigurra.heisenberg

import MapData.SourceData

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag

abstract class Schema[ObjectType <: Parsed[ObjectType] : ClassTag] {

  private val _fields = new ArrayBuffer[Field[Any]]
  private val _fieldsByName = new mutable.HashMap[String, Field[Any]]()
  private lazy val _fieldnames = fields.map(_.name).toSet

  private def viewField[T, FieldType <: Field[T]](f: FieldType): FieldType = {

    if (_fieldsByName.contains(f.name))
      throw InvalidSchemaUse(s"Duplicated field name '${f.name}. Cannot add field '$f' to Schema \n: $this", null)

    _fields += f
    _fieldsByName += f.name -> f
    f
  }

  protected def required[T: TypeTag : MapDataParser : MapDataProducer](name: String, default: => T = null.asInstanceOf[T]): FieldRequired[T] = {
    viewField[T, FieldRequired[T]](FieldRequired(name, Option(default)))
  }

  protected def optional[T: TypeTag : MapDataParser : MapDataProducer](name: String): FieldOption[T] = {
    viewField[Option[T], FieldOption[T]](FieldOption(name))
  }

  def contains(field: Field[Any]): Boolean = {
    fields.exists(_ eq field)
  }

  def fields: Seq[Field[Any]] = _fields

  def fieldNames: Set[String] = _fieldnames

  def field(name: String): Field[Any] = _fieldsByName(name)

  def apply(objectData: SourceData): ObjectType

  def marshal(fields: MapData*): ObjectType = MapParser.parse[ObjectType](MapData(fields: _*))

  val defaultParser: MapParser[ObjectType] = Parsed.Parser[ObjectType](apply(_).validate())

  implicit def parser: MapParser[ObjectType] = defaultParser

  override def toString: String = s"Schema (${getClass.getName}):\n\t${fields.mkString("\n\t")}"

  implicit def dynamic2parsed(dd: MapData): ObjectType = marshal(dd)

  def parse(sourceData: SourceData): ObjectType = MapParser.parse[ObjectType](sourceData)

}

case class InvalidSchemaUse(msg: String, cause: Throwable) extends RuntimeException(msg, cause)
