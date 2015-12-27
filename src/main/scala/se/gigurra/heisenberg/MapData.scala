package se.gigurra.heisenberg

import se.gigurra.heisenberg.MapData.SourceData

import scala.language.implicitConversions
import scala.reflect.runtime.universe._

case class PureMapData(source: SourceData = Map.empty) extends MapData

trait MapData {

  def source: SourceData

  def +[T : MapDataProducer](tuple: (FieldRequired[T], T)): MapData = {
    PureMapData(source + (tuple._1 --> tuple._2))
  }

  def +[T : MapDataProducer: FixErasure1](tuple: (FieldOption[T], Option[T])): MapData = {
    PureMapData(tuple._2 match {
      case Some(value) => source + (tuple._1 --> value)
      case None => source - tuple._1.name
    })
  }

  def +[T : MapDataProducer : FixErasure2](tuple: (FieldOption[T], T)): MapData = {
    PureMapData(source + (tuple._1 --> tuple._2))
  }

  def +(other: MapData): MapData = {
    PureMapData(source ++ other.source)
  }

  def contains(field: Field[_]): Boolean = {
    source.contains(field.name)
  }

  def rename[FieldType](oldNewFields: (Field[FieldType], Field[FieldType])): MapData = {
    val oldField: Field[FieldType] = oldNewFields._1
    val newField: Field[FieldType] = oldNewFields._2
    source.get(oldField.name) match {
      case Some(source) => this - oldField + (newField.name -> source)
      case None => this
    }
  }

  def -[FieldType](field: Field[FieldType]): MapData = {
    PureMapData(source - field.name)
  }

  def as[T : MapParser : WeakTypeTag]: T = MapParser.parse[T](source)

  def as[T <: Parsed[_] : MapParser : WeakTypeTag](schema: Schema[T]): T = as[T]

}

object MapData {
  type SourceData = Map[String, Any]

  def fromSource[T <: Parsed[_]](t: T): MapData = PureMapData(t.source)

  implicit def map2dynamicData(source: Map[String, Any]): MapData = PureMapData(source)

  implicit def dynamicdata2Map(dynamicData: MapData): SourceData = dynamicData.source

  implicit def tuple2DynamicData[T : MapDataProducer](tuple: (FieldRequired[T], T)): MapData = {
    MapData() + tuple
  }

  implicit def tuple2DynamicData[T : MapDataProducer : FixErasure1](tuple: (FieldOption[T], Option[T])): MapData = {
    MapData() + tuple
  }

  implicit def tuple2DynamicData[T : MapDataProducer : FixErasure2](tuple: (FieldOption[T], T)): MapData = {
    MapData() + tuple
  }

  implicit def stringRawValue2DynamicData(tuple: (String, Any)): MapData = {
    PureMapData(Map(tuple))
  }

  implicit def parsed2dynamic[T <: Parsed[_]](t: T): MapData = fromSource(t)

  def apply(dynamics: MapData*): MapData = {
    dynamics.fold(new PureMapData())((a, b) => a + b)
  }

}
