package se.gigurra.heisenberg

import org.scalatest._
import org.scalatest.mock._

import scala.util.{Failure, Success, Try}

class HeisenbergSpec
  extends WordSpec
  with MockitoSugar
  with Matchers
  with OneInstancePerTest {

  import TestTypes._


  "Heisenberg" should {

    "parse MyDynamic from Map" in {
      import SimpleTestType._
      val source = MapData(req_str -> "Hello", req_str_w_def -> "You too").source
      val instance = parse(source)
      instance.req_str shouldBe "Hello"
      instance.req_str_w_def shouldBe "You too"
    }

    "Remarshal MyDynamic from itself" in {
      import SimpleTestType._
      val source = MapData(req_str -> "Hello", req_str_w_def -> "You too").source
      val instance: SimpleTestType = parse(source)
      val instance2: SimpleTestType = instance.marshal(instance)
      instance shouldBe instance2
    }

    "Schema from parsed should be of correct type" in {
      import SimpleTestType._
      val instance: SimpleTestType = parse(MapData(req_str -> "Hello", req_str_w_def -> "You too"))
      val schema: SimpleTestType.type = instance.schema
      schema shouldBe an[SimpleTestType.type]
    }

    "parse Either[Int, String]" in {

      import ParseEitherTest._
      import EitherTestType._
      val stringSource = Map(intOrString.name -> "abc")
      val stringInstance = parse(stringSource)
      stringInstance.intOrString shouldBe a[Right[_, _]]
      stringInstance.intOrString.right.get shouldBe "abc"

      val intSource = Map(intOrString.name -> 123)
      val intInstance = parse(intSource)
      intInstance.intOrString shouldBe a[Left[_, _]]
      intInstance.intOrString.left.get shouldBe 123

      MapDataProducer.produce(stringInstance) shouldBe Map(intOrString.name -> "abc")
      MapDataProducer.produce(intInstance) shouldBe Map(intOrString.name -> 123)
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

    "fail to parse if wrong field type (String->Int)" in {
      import SimpleTestType._
      val source = Map(req_str.name -> "Hello", req_str_w_def.name -> 2)
      val result = Try(parse(source))
      result shouldBe a[Failure[_]]
      result.failed.get shouldBe a[MapDataParser.WrongType]

      println(result.failed.get.getMessage)
    }

    "fail to parse if wrong field type (String instead of Seq[Obj])" in {

      import OuterInnerTest._
      val source = Map("inners" -> "lalala")
      val result = Try(Outer.parse(source))
      result shouldBe a[Failure[_]]
      result.failed.get shouldBe a[MapDataParser.WrongType]

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

    "Handle generic types" in {
      import GenericsTest._
      {
        val schema = MyType.schema[Int]
        import schema._
        val source = MapData(t -> 42).source
        val o = MyType[Int](source)
        o.t shouldBe 42
      }
      {
        val schema = MyType.schema[String]
        import schema._
        val source = MapData(t -> "s").source
        val o = MyType[String](source)
        o.t shouldBe "s"
      }
    }

    "flatten with default values recursively" in {
      import NestedTestType._
      import SimpleTestType._
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
      import NestedTestType._
      import SimpleTestType._
      val innerSource1 = MapData(req_str -> "HelloInner1").source
      val innerSource2 = MapData(/*req_str -> "HelloInner2",*/ req_str_w_def -> "123321").source
      val source = Map(rq.name -> Seq(innerSource1, innerSource2))
      val result = Try(NestedTestType.parse(source))

      result shouldBe a[Failure[_]]
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

      import MigrateTest._
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


    "support field migration" in {
      import FieldMigrationTest._

      val data = Map("foo" -> 2)
      val instance1 = TestType(data)

      val oldData = Map("bar" -> "1")
      val instance2 = TestType(oldData)

      instance1.foo shouldBe instance2.foo
    }

    "support optionals as optionals and/or null" in {
      import OptionalField._

      val data1 = Map("foo" -> "my_foo", "bar" -> null, "foobar" -> Seq.empty)
      val data2 = Map("foo" -> "my_foo", "bar" -> None, "foobar" -> Seq.empty)
      val data3 = Map("foo" -> "my_foo", "foobar" -> Seq(Some("1"), None))
      val data4 = Map("foo" -> "my_foo", "foobar" -> Seq(Some("1"), null, Some("3")))
      val instance1 = TestType(data1)
      val instance2 = TestType(data2)
      val instance3 = TestType(data3)
      val instance4 = TestType(data4)

      instance1.bar shouldBe None
      instance2.bar shouldBe None
      instance3.bar shouldBe None
      instance4.bar shouldBe None

      instance3.foobar shouldBe Seq(Some("1"), None)
      instance4.foobar shouldBe Seq(Some("1"), None, Some("3"))

      val instance3b = TestType(instance3.flatten)
      val instance4b = TestType(instance4.flatten)

      instance3 shouldBe instance3b
      instance4 shouldBe instance4b

      instance1.flatten shouldBe (data1 - "bar")
      instance2.flatten shouldBe (data2 - "bar")
    }

    "support anonymous single field objects" should {

      "produce objects from correct input data" in {
        "Hello" shouldBe MapParser.parseSingleFieldObject[String](Map("some_field" -> "Hello"), "some_field")
        123 shouldBe MapParser.parseSingleFieldObject[Int](Map("data" -> 123))
      }

      "Fail on missing field" in {
        val result = Try(MapParser.parseSingleFieldObject[String](Map("some_field" -> "Hello"), "somxe_field"))
        result shouldBe a[Failure[_]]
        println(result.failed.get.getMessage)
      }

      "Fail on wrong field type" in {
        val result = Try(MapParser.parseSingleFieldObject[String](Map("some_field" -> 123), "some_field"))
        result shouldBe a[Failure[_]]
        println(result.failed.get.getMessage)
      }

    }

    "print schema" in {
      println(NestedTestType)
    }

    "automatically migrate new data to old schemas (=time-travel: backwards)" in {
      // TODO: Implement
    }

    "support inheritance" in {
      // TODO: Implement
    }

  }

}
