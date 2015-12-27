package se.gigurra.heisenberg

import MapData.SourceData

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.implicitConversions
import scala.reflect.runtime.universe.WeakTypeTag

abstract class Schema[T <: Parsed[_] : WeakTypeTag] {

  implicit val instance: this.type = this

  private val _fields = new ArrayBuffer[Field[Any]]
  private val _fieldsByName = new mutable.HashMap[String, Field[Any]]()
  private lazy val _fieldnames = fields.map(_.name).toSet

  private def viewField[F, FieldType <: Field[F]](f: FieldType): FieldType = {

    if (_fieldsByName.contains(f.name))
      throw InvalidSchemaUse(s"Duplicated field name '${f.name}. Cannot add field '$f' to Schema \n: $this", null)

    _fields += f
    _fieldsByName += f.name -> f
    f
  }

  protected def required[F: WeakTypeTag : MapDataParser : MapDataProducer](name: String, default: => F = null.asInstanceOf[F]): FieldRequired[F] = {
    viewField[F, FieldRequired[F]](FieldRequired(name, Option(default)))
  }

  protected def optional[F: WeakTypeTag : MapDataParser : MapDataProducer](name: String): FieldOption[F] = {
    viewField[Option[F], FieldOption[F]](FieldOption(name))
  }

  def contains(field: Field[Any]): Boolean = {
    fields.exists(_ eq field)
  }

  def fields: Seq[Field[Any]] = _fields

  def fieldNames: Set[String] = _fieldnames

  def field(name: String): Field[Any] = _fieldsByName(name)

  def apply(objectData: SourceData): T

  def marshal(fields: MapData*): T = MapParser.parse[T](MapData(fields: _*))

  val defaultParser: MapParser[T] = Parsed.Parser[T](apply(_).validate())

  implicit def parser: MapParser[T] = defaultParser

  override def toString: String = s"Schema (${getClass.getName}):\n\t${fields.mkString("\n\t")}"

  implicit def dynamic2parsed(dd: MapData): T = marshal(dd)

  def parse(sourceData: SourceData): T = MapParser.parse[T](sourceData)

}

case class InvalidSchemaUse(msg: String, cause: Throwable) extends RuntimeException(msg, cause)
