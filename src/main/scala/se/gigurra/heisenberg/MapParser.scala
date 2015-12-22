package se.gigurra.heisenberg

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

trait MapParser[T] {
  def parse(data: Map[String, Any]): T
}

object MapParser {

  def parse[T: MapParser : ClassTag](data: Map[String, Any]): T = {
    try {
      implicitly[MapParser[T]].parse(data)
    } catch {
      case e: MapDataParser.ParseError =>
        e.path = scala.reflect.classTag[T].runtimeClass.getSimpleName :: e.path
        throw e
    }
  }

  def parseSingleFieldObject[T: MapDataParser : ClassTag: TypeTag](data: Map[String, Any], fieldName: String = "data"): T = {

    data.get(fieldName) match {
      case Some(t) =>
        try {
          MapDataParser.parse[T](t, fieldName)
        } catch {
          case e: MapDataParser.ParseError =>
            e.path = "SingleFieldObject" :: e.path
            throw e
        }
      case None =>
        throw new MapDataParser.MissingField(fieldName, implicitly[TypeTag[T]].tpe.toString) {
          path = "SingleFieldObject" :: path
        }
    }
  }


}
