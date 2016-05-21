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

  private val observed = new ArrayBuffer[ObservedField[_]]
  private lazy val flattened: Map[String, Any] = doFlatten()

  protected def parse[FieldType](field: FieldOption[FieldType], orElse: => Option[FieldType]): Option[FieldType] = parseOptField(field, orElse)
  protected def parse[FieldType](field: FieldOption[FieldType]): Option[FieldType] = parseOptField(field, None)

  protected def parse[FieldType](field: FieldRequired[FieldType], orElse: => FieldType): FieldType = parseReqField(field, Some(orElse))
  protected def parse[FieldType](field: FieldRequired[FieldType]): FieldType = parseReqField(field, None)


  ////////////////////////////////////
  //  Overrideables
  //

  val source: SourceData


  ////////////////////////////////////
  //  Convenience ops
  //

  def observedFields: Seq[ObservedField[_]] = observed

  def flatten: SourceData = flattened

  def marshal(sources: MapData*)(implicit tag: WeakTypeTag[this.type]): this.type = schema.marshal(sources:_*).asInstanceOf[this.type]

  override def toString: String = flatten.toString

  override def hashCode(): Int = {
    flatten.hashCode()
  }

  override def equals(other: Any): Boolean = {
    other match {
      case other : Parsed[_]  => other.flatten == this.flatten
      case _                  => false
    }
  }

  ////////////////////////////////////
  //  Helpers
  //

  private def doFlatten(): Map[String, Any] = {
    (source ++ observed.map(v => v.name -> v.flatten)).filter(x => x._2 != null && x._2 != None)
  }

  private def parseOptField[FieldType](field: FieldOption[FieldType], orElse: => Option[FieldType]): Option[FieldType] = {
    val view = field.parse(source, orElse)
    view.foreach(observed += ObservedField(_, field.name, field.mapDataProducer))
    view
  }

  private def parseReqField[FieldType](field: FieldRequired[FieldType], orElse: => Option[FieldType]): FieldType = {
    val view = field.parse(source, orElse)
    observed += ObservedField(view, field.name, field.mapDataProducer)
    view
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

case class ObservedField[FieldType](view: FieldType, name: String, mapDataProducer: MapDataProducer[FieldType]) {
  def flatten: Any = mapDataProducer.produce(view)
}
