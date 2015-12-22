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

  case class SimpleTestType(source: SourceData) extends Parsed[SimpleTestType] {
    def schema = SimpleTestType
    val req_str = parse(schema.req_str)
    val req_str_w_def = parse(schema.req_str_w_def)
  }

  object SimpleTestType extends Schema[SimpleTestType] {
    val req_str = required[String]("a")
    val req_str_w_def = required[String]("b", "mydefault")
  }

  case class NestedTestType(source: SourceData) extends Parsed[NestedTestType] {
    def schema = NestedTestType
    val rq = parse(schema.rq)
    val df = parse(schema.df)
  }

  object NestedTestType extends Schema[NestedTestType] {
    val rq = required[Seq[SimpleTestType]]("rq")
    val df = required[Seq[SimpleTestType]]("df", Seq(SimpleTestType.marshal(SimpleTestType.req_str -> "123")))
  }

  "Dynamic" should {

    "parse MyDynamic from Map" in {
      import SimpleTestType._
      val source = MapData(req_str -> "Hello", req_str_w_def -> "You too").source
      val instance = parse(source)
      instance.req_str shouldBe "Hello"
      instance.req_str_w_def shouldBe "You too"
    }

    "preserve extra data in source and flattened formats" in {
      import SimpleTestType._

      val sourceData = Map(req_str.name -> "Hello", req_str_w_def.name -> "You too", "extra_field" -> "I'm here!")

      val source = MapData(sourceData).source
      val instance = parse(source)
      instance.req_str shouldBe "Hello"
      instance.req_str_w_def shouldBe "You too"
      instance.source shouldBe sourceData
      instance.flatten shouldBe sourceData
    }

    "fail to parse if missing required data" in {
      import SimpleTestType._
      val source = MapData(req_str_w_def -> "You too").source
      val result = Try(parse(source))
      result shouldBe a[Failure[_]]
      result.failed.get shouldBe a[MapDataParser.MissingField]

      println(result.failed.get.getMessage)
    }

    "fail to parse if wrong field type" in {
      import SimpleTestType._
      val source = Map(req_str.name -> "Hello", req_str_w_def.name -> 2)
      val result = Try(parse(source))
      result shouldBe a[Failure[_]]
      result.failed.get shouldBe a[MapDataParser.WrongType]

      println(result.failed.get.getMessage)
    }

    "view default values when source is missing data" in {
      import SimpleTestType._
      val source = MapData(req_str -> "Hello").source
      val instance = parse(source)
      instance.req_str shouldBe "Hello"
      instance.req_str_w_def shouldBe "mydefault"
    }

    "flatten with default values" in {
      import SimpleTestType._
      val source = MapData(req_str -> "Hello").source
      val instance = parse(source)
      instance.flatten shouldBe MapData(req_str -> "Hello", req_str_w_def -> "mydefault").source
    }

    "flatten with default values recursively" in {
      import SimpleTestType._
      import NestedTestType._
      val innerSource1 = MapData(req_str -> "HelloInner1").source
      val innerSource2 = MapData(req_str -> "HelloInner2", req_str_w_def -> "123321").source
      val source = Map(rq.name -> Seq(innerSource1, innerSource2))
      val instance = NestedTestType.parse(source)
      val reference = Map(
        rq.name -> Seq(Map(req_str.name -> "HelloInner1", req_str_w_def.name -> "mydefault"), Map(req_str.name -> "HelloInner2", req_str_w_def.name -> "123321")),
        df.name -> Seq(Map(req_str.name -> "123", req_str_w_def.name -> "mydefault"))
      )
      instance.flatten shouldBe reference
    }

    "fail parse when inner data is invalid" in {
      import SimpleTestType._
      import NestedTestType._
      val innerSource1 = MapData(req_str -> "HelloInner1").source
      val innerSource2 = MapData(/*req_str -> "HelloInner2",*/ req_str_w_def -> "123321").source
      val source = Map(rq.name -> Seq(innerSource1, innerSource2))
      val result = Try(NestedTestType.parse(source))

      result shouldBe a [Failure[_]]
      result.failed.get shouldBe a[MapDataParser.MissingField]

      println(result.failed.get.getMessage)
    }

    "produce maps with default values included for missing source values" in {
      import SimpleTestType._
      val source = MapData(req_str -> "Hello").source
      val instance = parse(source)
      MapProducer.produce(instance) shouldBe MapData(req_str -> "Hello", req_str_w_def -> "mydefault").source
    }

    "automatically migrate old data to new schemas (=time-travel: forwards)" in {

      object UpdatedTestType extends Schema[UpdatedTestType] {
        val req_str = required[String]("ax")
        val req_str_w_def = required[String]("bx", "mydefaultx")

        import MapData._

        val migrator: Migrator[UpdatedTestType, SimpleTestType] = { old =>
          old.flatten
            .rename(SimpleTestType.req_str -> UpdatedTestType.req_str)
            .rename(SimpleTestType.req_str_w_def -> UpdatedTestType.req_str_w_def)
            .as[UpdatedTestType]
        }

        override val parser = Migration(UpdatedTestType.defaultParser, SimpleTestType.parser, migrator)
      }
      case class UpdatedTestType(source: SourceData)
        extends Parsed[UpdatedTestType] {
        def schema = UpdatedTestType
        val req_str = parse(schema.req_str)
        val req_str_w_def = parse(schema.req_str_w_def)
      }

      val source = MapData(SimpleTestType.req_str -> "Hello").source
      val m1 = SimpleTestType.parse(source)
      m1.req_str shouldBe "Hello"
      m1.req_str_w_def shouldBe "mydefault"


      val m2FromMigrator = UpdatedTestType.migrator.apply(m1)
      val m2Parsed = UpdatedTestType.parse(source)

      m2FromMigrator shouldBe m2Parsed
      m2Parsed.source.contains("ax") shouldBe true
      m2Parsed.source.contains("bx") shouldBe true
      m2Parsed.source.contains("a") shouldBe false
      m2Parsed.source.contains("b") shouldBe false

    }

    "throw if trying to parse a field more than once" in {

      object SomeTestType extends Schema[SomeTestType] {
        val b = required[Seq[SimpleTestType]]("b", Seq(SimpleTestType.marshal(SimpleTestType.req_str -> "123")))
      }

      case class SomeTestType(source: SourceData) extends Parsed[SomeTestType] {
        def schema = SomeTestType
        val df1 = parse(schema.b)
        val df2 = parse(schema.b)
      }

      val result = Try(SomeTestType(Map.empty))
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

      case class SomeTestType(source: SourceData) extends Parsed[SomeTestType] {
        def schema = SomeSchema
      }

      lazy val SomeSchema = new Schema[SomeTestType] {
        val a1 = optional[String]("a")
        val a2 = optional[String]("a")
        def apply(d: Map[String, Any]): SomeTestType = marshal()
      }

      val result = Try(SomeSchema)
      result shouldBe a [Failure[_]]
      result.failed.get shouldBe a [InvalidSchemaUse]

    }

    "Forgetting to parse a field should throw when parsing" in {

      object SomeTestType extends Schema[SomeTestType] {
        val a = required[Seq[SimpleTestType]]("a")
        val b = required[Seq[SimpleTestType]]("b", Seq(SimpleTestType.marshal(SimpleTestType.req_str -> "123")))
      }

      case class SomeTestType(source: SourceData) extends Parsed[SomeTestType] {
        def schema = SomeTestType
        val df = parse(schema.b)
      }

      val result = Try(SomeTestType.parse(Map.empty))

      result shouldBe a[Failure[_]]
      result.failed.get shouldBe a[InvalidSchemaUse]
    }

    "Forgetting to parse a field should throw when flattening" in {

      object SomeTestType extends Schema[SomeTestType] {
        val a = required[Seq[SimpleTestType]]("a")
        val b = required[Seq[SimpleTestType]]("b", Seq(SimpleTestType.marshal(SimpleTestType.req_str -> "123")))
      }

      case class SomeTestType(source: SourceData) extends Parsed[SomeTestType] {
        def schema = SomeTestType
        val df = parse(schema.b)
      }

      val x = SomeTestType(Map.empty)
      val result = Try(x.flatten)
      result shouldBe a[Failure[_]]
      result.failed.get shouldBe a[InvalidSchemaUse]
    }

    "print schema" in {
      println(NestedTestType)
    }

  }

}
