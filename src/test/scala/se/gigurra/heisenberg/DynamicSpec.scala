package se.gigurra.heisenberg

import MapData.SourceData
import Migration.Migrator
import org.scalatest._
import org.scalatest.mock._

import scala.util.{Failure, Try}

class DynamicSpec
  extends WordSpec
  with MockitoSugar
  with Matchers
  with OneInstancePerTest {

  case class MyMap(source: SourceData) extends Parsed[MyMap] {
    def schema = MyMap
    val req_str = parse(schema.req_str)
    val req_str_w_def = parse(schema.req_str_w_def)
  }

  object MyMap extends Schema[MyMap] {
    val req_str = required[String]("a")
    val req_str_w_def = required[String]("b", "mydefault")
  }

  case class MyMapRec(source: SourceData) extends Parsed[MyMapRec] {
    def schema = MyMapRec
    val rq = parse(schema.rq)
    val df = parse(schema.df)
  }

  object MyMapRec extends Schema[MyMapRec] {
    val rq = required[Seq[MyMap]]("rq")
    val df = required[Seq[MyMap]]("df", Seq(MyMap.marshal(MyMap.req_str -> "123")))
  }

  "Dynamic" should {

    "parse MyDynamic from Map" in {
      import MyMap._
      val source = MapData(req_str -> "Hello", req_str_w_def -> "You too").source
      val instance = parse(source)
      instance.req_str shouldBe "Hello"
      instance.req_str_w_def shouldBe "You too"
    }

    "preserve extra data in source and flattened formats" in {
      import MyMap._

      val sourceData = Map(req_str.name -> "Hello", req_str_w_def.name -> "You too", "extra_field" -> "I'm here!")

      val source = MapData(sourceData).source
      val instance = parse(source)
      instance.req_str shouldBe "Hello"
      instance.req_str_w_def shouldBe "You too"
      instance.source shouldBe sourceData
      instance.flatten shouldBe sourceData
    }

    "fail to parse if missing required data" in {
      import MyMap._
      val source = MapData(req_str_w_def -> "You too").source
      val result = Try(parse(source))
      result shouldBe a[Failure[_]]
      result.failed.get shouldBe a[MapDataParser.MissingField]

      println(result.failed.get.getMessage)
    }

    "fail to parse if wrong field type" in {
      import MyMap._
      val source = Map(req_str.name -> "Hello", req_str_w_def.name -> 2)
      val result = Try(parse(source))
      result shouldBe a[Failure[_]]
      result.failed.get shouldBe a[MapDataParser.WrongType]

      println(result.failed.get.getMessage)
    }

    "view default values when source is missing data" in {
      import MyMap._
      val source = MapData(req_str -> "Hello").source
      val instance = parse(source)
      instance.req_str shouldBe "Hello"
      instance.req_str_w_def shouldBe "mydefault"
    }

    "flatten with default values" in {
      import MyMap._
      val source = MapData(req_str -> "Hello").source
      val instance = parse(source)
      instance.flatten shouldBe MapData(req_str -> "Hello", req_str_w_def -> "mydefault").source
    }

    "flatten with default values recursively" in {
      import MyMap._
      import MyMapRec._
      val innerSource1 = MapData(req_str -> "HelloInner1").source
      val innerSource2 = MapData(req_str -> "HelloInner2", req_str_w_def -> "123321").source
      val source = Map(rq.name -> Seq(innerSource1, innerSource2))
      val instance = MyMapRec.parse(source)
      val reference = Map(
        rq.name -> Seq(Map(req_str.name -> "HelloInner1", req_str_w_def.name -> "mydefault"), Map(req_str.name -> "HelloInner2", req_str_w_def.name -> "123321")),
        df.name -> Seq(Map(req_str.name -> "123", req_str_w_def.name -> "mydefault"))
      )
      instance.flatten shouldBe reference
    }

    "fail parse when inner data is invalid" in {
      import MyMap._
      import MyMapRec._
      val innerSource1 = MapData(req_str -> "HelloInner1").source
      val innerSource2 = MapData(/*req_str -> "HelloInner2",*/ req_str_w_def -> "123321").source
      val source = Map(rq.name -> Seq(innerSource1, innerSource2))
      val result = Try(MyMapRec.parse(source))

      result shouldBe a [Failure[_]]
      result.failed.get shouldBe a[MapDataParser.MissingField]

      println(result.failed.get.getMessage)
    }

    "produce maps with default values included for missing source values" in {
      import MyMap._
      val source = MapData(req_str -> "Hello").source
      val instance = parse(source)
      MapProducer.produce(instance) shouldBe MapData(req_str -> "Hello", req_str_w_def -> "mydefault").source
    }

    "automatically migrate old data to new schemas (=time-travel: forwards)" in {

      object MyMap2 extends Schema[MyMap2] {
        val req_str = required[String]("ax")
        val req_str_w_def = required[String]("bx", "mydefaultx")

        import MapData._

        val migrator: Migrator[MyMap2, MyMap] = { old =>
          old.flatten
            .rename(MyMap.req_str -> MyMap2.req_str)
            .rename(MyMap.req_str_w_def -> MyMap2.req_str_w_def)
            .as[MyMap2]
        }

        override val parser = Migration(defaultParser, MyMap.parser, migrator)
      }
      case class MyMap2(source: SourceData)
        extends Parsed[MyMap2] {
        def schema = MyMap2
        val req_str = parse(schema.req_str)
        val req_str_w_def = parse(schema.req_str_w_def)
      }

      val source = MapData(MyMap.req_str -> "Hello").source
      val m1 = MyMap.parse(source)
      m1.req_str shouldBe "Hello"
      m1.req_str_w_def shouldBe "mydefault"


      val m2FromMigrator = MyMap2.migrator.apply(m1)
      val m2Parsed = MyMap2.parse(source)

      m2FromMigrator shouldBe m2Parsed
      m2Parsed.source.contains("ax") shouldBe true
      m2Parsed.source.contains("bx") shouldBe true
      m2Parsed.source.contains("a") shouldBe false
      m2Parsed.source.contains("b") shouldBe false

    }

    "throw if trying to parse a field more than once" in {

      object X extends Schema[X] {
        val b = required[Seq[MyMap]]("b", Seq(MyMap.marshal(MyMap.req_str -> "123")))
      }

      case class X(source: SourceData) extends Parsed[X] {
        def schema = X
        val df1 = parse(schema.b)
        val df2 = parse(schema.b)
      }

      val result = Try(X(Map.empty))
      result shouldBe a[Failure[_]]
      result.failed.get shouldBe a[InvalidSchemaUse]
    }

    "automatically migrate new data to old schemas (=time-travel: backwards)" in {
      // TODO: Implement
    }

    "support inheritance" in {
      // TODO: Implement
    }

    "support anonymous single field objects" should {

      "produce objects from correct input data" in {
        "Hello" shouldBe MapParser.parseSingleFieldObject[String](Map("some_field" -> "Hello"), "some_field")
        123 shouldBe MapParser.parseSingleFieldObject[Int](Map("data" -> 123))
      }

      "Fail on missing field" in {
        val result = Try(MapParser.parseSingleFieldObject[String](Map("some_field" -> "Hello"), "somxe_field"))
        result shouldBe a [Failure[_]]
        println(result.failed.get.getMessage)
      }

      "Fail on wrong field type" in {
        val result = Try(MapParser.parseSingleFieldObject[String](Map("some_field" -> 123), "some_field"))
        result shouldBe a [Failure[_]]
        println(result.failed.get.getMessage)
      }

    }

    "fail to create schema with duplicate field names" in {

      case class X(source: SourceData) extends Parsed[X] {
        def schema = xSchema
      }

      lazy val xSchema = new Schema[X] {
        val a1 = optional[String]("a")
        val a2 = optional[String]("a")
        def apply(d: Map[String, Any]): X = marshal()
      }

      val result = Try(xSchema)
      result shouldBe a [Failure[_]]
      result.failed.get shouldBe a [InvalidSchemaUse]

    }

    "Forgetting to parse a field should throw when parsing" in {

      object X extends Schema[X] {
        val a = required[Seq[MyMap]]("a")
        val b = required[Seq[MyMap]]("b", Seq(MyMap.marshal(MyMap.req_str -> "123")))
      }

      case class X(source: SourceData) extends Parsed[X] {
        def schema = X
        val df = parse(schema.b)
      }

      val result = Try(X.parse(Map.empty))

      result shouldBe a[Failure[_]]
      result.failed.get shouldBe a[InvalidSchemaUse]
    }

    "Forgetting to parse a field should throw when flattening" in {

      object X extends Schema[X] {
        val a = required[Seq[MyMap]]("a")
        val b = required[Seq[MyMap]]("b", Seq(MyMap.marshal(MyMap.req_str -> "123")))
      }

      case class X(source: SourceData) extends Parsed[X] {
        def schema = X
        val df = parse(schema.b)
      }

      val x = X(Map.empty)
      val result = Try(x.flatten)
      result shouldBe a[Failure[_]]
      result.failed.get shouldBe a[InvalidSchemaUse]
    }

    "print schema" in {
      println(MyMapRec)
    }

  }

}
