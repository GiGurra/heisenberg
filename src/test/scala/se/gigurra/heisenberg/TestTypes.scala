package se.gigurra.heisenberg

import se.gigurra.heisenberg.MapData._
import se.gigurra.heisenberg.Migration._

/**
  * Created by kjolh on 12/27/2015.
  */
object TestTypes {

  case class SimpleTestType(source: SourceData) extends Parsed[SimpleTestType.type] {
    val req_str = parse(schema.req_str)
    val req_str_w_def = parse(schema.req_str_w_def)
  }

  object SimpleTestType extends Schema[SimpleTestType] {
    val req_str = required[String]("a")
    val req_str_w_def = required[String]("b", "mydefault")
  }

  case class NestedTestType(source: SourceData) extends Parsed[NestedTestType.type] {
    val rq = parse(schema.rq)
    val df = parse(schema.df)
  }

  object NestedTestType extends Schema[NestedTestType] {
    val rq = required[Seq[SimpleTestType]]("rq")
    val df = required[Seq[SimpleTestType]]("df", Seq(SimpleTestType.marshal(SimpleTestType.req_str -> "123")))
  }

  object ParseEitherTest {

    case class EitherTestType(source: SourceData) extends Parsed[EitherTestType.type] {
      val intOrString = parse(schema.intOrString)
    }

    object EitherTestType extends Schema[EitherTestType] {
      val intOrString = required[Either[Int, String]]("intOrString")
    }

  }

  object OuterInnerTest {

    object Inner extends Schema[Inner] {
      val data = required[String]("data")
    }

    case class Inner(source: SourceData) extends Parsed[Inner.type] {
      val data = parse(schema.data)
    }

    object Outer extends Schema[Outer] {
      val inners = required[Seq[Inner]]("inners")
    }

    case class Outer(source: SourceData) extends Parsed[Outer.type] {
      val inners = parse(schema.inners)
    }

  }

  object MigrateTest {

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

      override lazy val parser = Migration.parser(
        UpdatedTestType.defaultParser,
        SimpleTestType.parser,
        migrator,
        warnOnMigrate = true
      )
    }
    case class UpdatedTestType(source: SourceData) extends Parsed[UpdatedTestType.type] {
      val req_str = parse(schema.req_str)
      val req_str_w_def = parse(schema.req_str_w_def)
    }

  }

  object ForgetToParseFieldTest {

    object X extends Schema[X] {
      val a = required[Seq[SimpleTestType]]("a")
      val b = required[Seq[SimpleTestType]]("b", Seq(SimpleTestType.marshal(SimpleTestType.req_str -> "123")))
    }

    case class X(source: SourceData) extends Parsed[X.type] {
      val df = parse(schema.b)
    }

  }

  object ParseFieldMoreThanOnceTest {

    object SomeTestType extends Schema[SomeTestType] {
      val b = required[Seq[SimpleTestType]]("b", Seq(SimpleTestType.marshal(SimpleTestType.req_str -> "123")))
    }

    case class SomeTestType(source: SourceData) extends Parsed[SomeTestType.type] {
      val df1 = parse(schema.b)
      val df2 = parse(schema.b)
    }

  }

}

object FailOnLoad {
  case class SomeTestType(source: SourceData) extends Parsed[SomeSchema.type] {
  }

  object SomeSchema extends Schema[SomeTestType] {
    val a1 = optional[String]("a")
    val a2 = optional[String]("a")
    def apply(d: Map[String, Any]): SomeTestType = marshal()
  }

}
