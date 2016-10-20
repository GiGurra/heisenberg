package com.github.gigurra.heisenberg

import scala.reflect.runtime.universe._

trait MapParser[T] {
  def parse(data: Map[String, Any]): T
}

object MapParser {

  def parse[T: MapParser : WeakTypeTag](data: Map[String, Any]): T = {
    try {
      implicitly[MapParser[T]].parse(data)
    } catch {
      case e: MapDataParser.Error =>
        e.path = weakTypeTag[T].tpe.toString.split('.').last :: e.path
        throw e
    }
  }

  def parseSingleFieldObject[T: MapDataParser : WeakTypeTag](data: Map[String, Any], fieldName: String = "data"): T = {

    data.get(fieldName) match {
      case Some(t) =>
        try {
          MapDataParser.parse[T](t, fieldName)
        } catch {
          case e: MapDataParser.Error =>
            e.path = "SingleFieldObject" :: e.path
            throw e
        }
      case None =>
        throw new MapDataParser.MissingField(fieldName, implicitly[WeakTypeTag[T]].tpe.toString) {
          path = "SingleFieldObject" :: path
        }
    }
  }


}
