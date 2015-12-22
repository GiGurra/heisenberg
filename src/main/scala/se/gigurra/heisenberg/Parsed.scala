package se.gigurra.heisenberg

import MapData.SourceData

import scala.collection.mutable.ArrayBuffer
import scala.language.implicitConversions
import scala.reflect.runtime.universe._

trait Parsed[T <: Parsed[T]] extends MapData { _: T =>

  ////////////////////////////////////
  //  Ctor API
  //

  private var validated = false
  private val parsed = new ArrayBuffer[Field[Any]]
  private val observed = new ArrayBuffer[ObservedField[Any]]
  private lazy val observedByName: Map[String, ObservedField[Any]] = observed.map(v => v.name -> v).toMap
  private lazy val flattened: Map[String, Any] = doFlatten()

  protected def parse[FieldType : MapDataProducer](field: FieldOption[FieldType]): Option[FieldType] = {
    validateFieldOnParse(field)
    val view = field.parse(source)
    view.foreach(observed += ObservedField(_, field))
    parsed += field
    view
  }

  protected def parse[FieldType : MapDataProducer](field: FieldRequired[FieldType]): FieldType = {
    validateFieldOnParse(field)
    val view = field.parse(source)
    observed += ObservedField(view, field)
    parsed += field
    view
  }


  ////////////////////////////////////
  //  Overrideables
  //

  val source: SourceData

  def schema: Schema[T]


  ////////////////////////////////////
  //  Convenience ops
  //

  def observedFields: Seq[ObservedField[_]] = observed

  def observedField[FieldType <: Any](name: String) = observedByName(name).view.asInstanceOf[FieldType]

  def flatten: SourceData = flattened

  def marshal(sources: MapData*)(implicit tag: WeakTypeTag[T]) = schema.marshal(sources:_*)

  override def toString: String = flatten.toString

  def validate(): T = {
    if (!validated) {
      val parsedNames = parsed.map(_.name).toSet
      val schemaNames = schema.fieldNames
      validate(parsedNames == schemaNames, s"Not all fields parsed, missing: [${(schemaNames -- parsedNames).mkString(", ")}]")
      validated = true
    }
    this
  }

  override def hashCode(): Int = {
    flatten.hashCode()
  }

  override def equals(other: Any): Boolean = {
    other match {
      case null              => false
      case other : Parsed[_] => other.flatten == this.flatten
      case _                 => false
    }
  }

  ////////////////////////////////////
  //  Helpers
  //

  private def doFlatten(): Map[String, Any] = {
    validate()
    source ++ observed.map(v => v.name -> v.flatten)
  }

  private def validate(condition: Boolean, msg: => String, cause: => Throwable = null): Unit = {
    if (!condition) {
      throw InvalidSchemaUse(msg, cause)
    }
  }

  private def validateFieldOnParse(field: Field[_]): Unit =  {
    validate(schema.contains(field), s"Attempted to parse field '$field' not part of schema $schema")
    validate(!parsed.exists(_ eq field), s"Attempted to parse fields ${field.name} twice in ${this}, Schema:\n$schema")
  }

}

object Parsed {

  object Parser {
    def apply[T <: Parsed[T]](applyMethod: SourceData => T) = new MapParser[T] {
      def parse(data: SourceData): T = applyMethod(data)
    }
  }

  implicit def mapDataParser[T <: Parsed[T] : MapParser: WeakTypeTag] = new MapDataParser[T] {
    override def parse(field: Any): T = field match {
      case _: Map[_, _] => MapParser.parse[T](field.asInstanceOf[Map[String, Any]])
      case _ => throw MapDataParser.WrongType(expect = s"Map[String, Any] (to produce a ${weakTypeTag[T].tpe})", was = MapDataParser.clsOf(field))
    }
  }

  implicit def mapProducer[T <: Parsed[T]] = new MapProducer[T] {
    override def produce(t: T): SourceData = t.flatten
  }

  implicit def mapDataProducer[T <: Parsed[T] : MapProducer] = new MapDataProducer[T] {
    override def produce(t: T): SourceData = implicitly[MapProducer[T]].produce(t)
  }

}

case class ObservedField[+FieldType : MapDataProducer](view: FieldType, field: Field[Any]) {
  def flatten: Any = MapDataProducer.produce(view)
  def name: String = field.name
}
