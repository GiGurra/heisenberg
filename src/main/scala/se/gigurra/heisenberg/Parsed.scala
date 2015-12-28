package se.gigurra.heisenberg

import MapData.SourceData

import scala.collection.mutable.ArrayBuffer
import scala.language.implicitConversions
import scala.reflect.runtime.universe._

abstract class Parsed[S <: Schema[_]](implicit val schema: S)
  extends MapData {

  ////////////////////////////////////
  //  Ctor API
  //

  private val observed = new ArrayBuffer[ObservedField[Any]]
  private lazy val observedByName: Map[String, ObservedField[Any]] = observed.map(v => v.name -> v).toMap
  private lazy val flattened: Map[String, Any] = doFlatten()

  protected def parse[FieldType : MapDataProducer](field: FieldOption[FieldType], orElse: => Option[FieldType]): Option[FieldType] = {
    validateFieldOnParse(field)
    val view = field.parse(source, orElse)
    view.foreach(observed += ObservedField(_, field))
    view
  }
  protected def parse[FieldType : MapDataProducer](field: FieldOption[FieldType]): Option[FieldType] = parse(field, null.asInstanceOf[Option[FieldType]])

  protected def parse[FieldType : MapDataProducer](field: FieldRequired[FieldType], orElse: => FieldType): FieldType = {
    validateFieldOnParse(field)
    val view = field.parse(source, Option(orElse))
    observed += ObservedField(view, field)
    view
  }
  protected def parse[FieldType : MapDataProducer](field: FieldRequired[FieldType]): FieldType = parse(field, null.asInstanceOf[FieldType])


  ////////////////////////////////////
  //  Overrideables
  //

  val source: SourceData


  ////////////////////////////////////
  //  Convenience ops
  //

  def observedFields: Seq[ObservedField[_]] = observed

  def observedField[FieldType <: Any](name: String) = observedByName(name).view.asInstanceOf[FieldType]

  def flatten: SourceData = flattened

  def marshal(sources: MapData*)(implicit tag: WeakTypeTag[this.type]): this.type = schema.marshal(sources:_*).asInstanceOf[this.type]

  override def toString: String = flatten.toString

  override def hashCode(): Int = {
    flatten.hashCode()
  }

  override def equals(other: Any): Boolean = {
    other match {
      case null             => false
      case other : Parsed[_]   => other.flatten == this.flatten
      case _                => false
    }
  }

  ////////////////////////////////////
  //  Helpers
  //

  private def doFlatten(): Map[String, Any] = {
    source ++ observed.map(v => v.name -> v.flatten)
  }

  private def validate(condition: Boolean, msg: => String, cause: => Throwable = null): Unit = {
    if (!condition) {
      throw InvalidSchemaUse(msg, cause)
    }
  }

  private def validateFieldOnParse(field: Field[_]): Unit =  {
    validate(schema.contains(field), s"Attempted to parse field '$field' not part of schema $schema")
  }

}

object Parsed {

  object Parser {
    def apply[T <: Parsed[_]](applyMethod: SourceData => T) = new MapParser[T] {
      def parse(data: SourceData): T = applyMethod(data)
    }
  }

  implicit def mapDataParser[T <: Parsed[_] : MapParser: WeakTypeTag] = new MapDataParser[T] {
    override def parse(field: Any): T = field match {
      case _: Map[_, _] => MapParser.parse[T](field.asInstanceOf[Map[String, Any]])
      case _ => throw MapDataParser.WrongType(expect = s"Map[String, Any] (to produce a ${weakTypeTag[T].tpe})", was = MapDataParser.clsOf(field))
    }
  }

  implicit def mapProducer[T <: Parsed[_]] = new MapProducer[T] {
    override def produce(t: T): SourceData = t.flatten
  }

  implicit def mapDataProducer[T <: Parsed[_] : MapProducer] = new MapDataProducer[T] {
    override def produce(t: T): SourceData = implicitly[MapProducer[T]].produce(t)
  }

}

case class ObservedField[+FieldType : MapDataProducer](view: FieldType, field: Field[Any]) {
  def flatten: Any = MapDataProducer.produce(view)
  def name: String = field.name
}
