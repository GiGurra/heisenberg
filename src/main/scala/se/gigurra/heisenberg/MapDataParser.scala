package se.gigurra.heisenberg

import scala.reflect.runtime.universe._
import scala.util.{Failure, Success, Try}

trait MapDataParser[T] {
  def parse(field: Any): T
}

object MapDataParser {

  sealed abstract class ParseError(msg: String, cause: Throwable = null, var path: List[String] = Nil) extends RuntimeException(msg, cause) {
    override def getMessage: String = {
      s"Parsing failed for '${path.mkString(".")}': $msg"
    }
  }
  case class WrongType(expect: String, was: String) extends ParseError(s"Unable to parse field. Expected data of type $expect, but was a $was")
  case class MissingField(name: String, typ: String) extends ParseError(s"Missing field: .$name ($typ)", null, List(name))

  def parse[T: MapDataParser](data: Any, path: String): T = {
    val parser = implicitly[MapDataParser[T]]
    try {
      parser.parse(data)
    } catch {
      case e: ParseError =>
        e.path = path :: e.path
        throw e
    }
  }

  implicit val parsingByte = new MapDataParser[Byte] {
    def parse(field: Any) = {
      field match {
        case field: Number => field.byteValue()
        case _ => throw WrongType(expect = "Int", was = clsOf(field))
      }
    }
  }

  implicit val parsingShort = new MapDataParser[Short] {
    def parse(field: Any) = {
      field match {
        case field: Number => field.shortValue()
        case _ => throw WrongType(expect = "Int", was = clsOf(field))
      }
    }
  }

  implicit val parsingInt = new MapDataParser[Int] {
    def parse(field: Any) = {
      field match {
        case field: Number => field.intValue()
        case _ => throw WrongType(expect = "Int", was = clsOf(field))
      }
    }
  }

  implicit val parsingLong = new MapDataParser[Long] {
    def parse(field: Any) = {
      field match {
        case field: Number => field.longValue()
        case _ => throw WrongType(expect = "Long", was = clsOf(field))
      }
    }
  }

  implicit val parsingBigint = new MapDataParser[BigInt] {
    def parse(field: Any) = {
      field match {
        case field: BigInt => field
        case field: java.math.BigInteger => field
        case field: Number => field.longValue()
        case _ => throw WrongType(expect = "Long", was = clsOf(field))
      }
    }
  }

  implicit val parsingBoolean = new MapDataParser[Boolean] {
    def parse(field: Any) = {
      field match {
        case field: Boolean => field
        case _ => throw WrongType(expect = "Boolean", was = clsOf(field))
      }
    }
  }

  implicit val parsingFloat = new MapDataParser[Float] {
    def parse(field: Any) = {
      field match {
        case field: Number => field.floatValue()
        case _ => throw WrongType(expect = "Float", was = clsOf(field))
      }
    }
  }

  implicit val parsingDouble = new MapDataParser[Double] {
    def parse(field: Any) = {
      field match {
        case field: Number => field.doubleValue()
        case _ => throw WrongType(expect = "Double", was = clsOf(field))
      }
    }
  }

  implicit val parsingString = new MapDataParser[String] {
    def parse(field: Any) = {
      field match {
        case field: String => field
        case _ => throw WrongType(expect = "String", was = clsOf(field))
      }
    }
  }

  implicit def parsingSeq[T: MapDataParser : WeakTypeTag] = new MapDataParser[Seq[T]] {
    val elementTransformer = (d: Any) => MapDataParser.parse[T](d, "[seq_element]")
    def parse(field: Any) = {
      field match {
        case field: Iterable[_] => field.map(elementTransformer).toSeq
        case _ => throw WrongType(expect = weakTypeTag[Seq[T]].tpe.toString, was = clsOf(field))
      }
    }
  }

  implicit def parsingEither[L: MapDataParser : WeakTypeTag, R: MapDataParser : WeakTypeTag] = new MapDataParser[Either[L, R]] {
    def parse(field: Any) = {
      Try(MapDataParser.parse[L](field, "[left_either]")) match {
        case Success(result) => Left[L, R](result)
        case Failure(_) => Try(MapDataParser.parse[R](field, "[right_either]")) match {
          case Success(result) => Right[L, R](result)
          case Failure(_) =>
            throw WrongType(expect = weakTypeTag[Either[L, R]].tpe.toString, was = clsOf(field))
        }
      }
    }
  }

  implicit def parsingSet[T: MapDataParser : WeakTypeTag] = new MapDataParser[Set[T]] {
    val elementTransformer = (d: Any) => MapDataParser.parse[T](d, "{set_element}")
    def parse(field: Any) = {
      field match {
        case field: Iterable[_] => field.map(elementTransformer).toSet
        case _ => throw WrongType(expect = weakTypeTag[Set[T]].tpe.toString, was = clsOf(field))
      }
    }
  }

  implicit def parsingMap[T: MapDataParser : WeakTypeTag] = new MapDataParser[Map[String, T]] {
    def parse(field: Any) = {
      field match {
        case field: Map[_, _] => field.asInstanceOf[Map[String, Any]].map(kv => kv._1 -> MapDataParser.parse[T](kv._2, kv._1))
        case _ => throw WrongType(expect = weakTypeTag[Map[String, T]].tpe.toString, was = clsOf(field))
      }
    }
  }

  def clsOf(x: Any): String  = {
    x match {
      case null => "<null>"
      case _ => x.getClass.toString
    }
  }

}
