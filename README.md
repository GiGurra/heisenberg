# Heisenberg
A scala library for handling dynamic and evolving [key,value] data when you're uncertain about its model, if there is no model, or you just don't care. - Get the best of both dynamic and static worlds.

* Heisenberg is very much WIP.. Examples & doc may come later.
  * All suggestions appreciated
  * Yet to decide on API design and what features Heisenberg should/not include

## Heisenberg helps with (w/o data loss)
* Persistence of partially defined and evolving data
* Routing of partially defined and evolving data
* Statically typed manipulation of dynamic data
* Migration & consolidation of dynamic data
* Validation of dynamic data


## Key features
* Selective parsing/validation without truncating source data
* Mapping [key,value] <-> [your class]a without loss of information
* Support custom types & representations
* Support fields of inconsistent types (e.g. x.a sometimes int, sometimes string)
* Automatic data model migration (field name changes, semantic changes etc.)
* [Type classes](https://twitter.github.io/scala_school/advanced-types.html) instead of reflection
* No code generation (just pure Scala)
* Custom arbitrary data validation (self-contained)
* Simple API


## Compatibility
* Json (e.g. json4s, ..)
* Schemaless NoSql databases (e.g. mongodb, couchbase, ..)
* Any Key-Value data, really.. 
  * currently anything that can be viewed as Map[String, Any]


## Examples

Below are some simple examples showing how to use Heisenberg


### Parsing dynamic data

```scala

val data = Map("a" -> "lalala", "b" -> 123, "extra_data" -> Seq(2,3,4))

object MyType extends Schema[MyType] {
 val foo = required[String]("a", default = "foo_default") // Creates a Field of type String
 val bar = optional[Int]("b") // Creates a Field of type Int
}

// Make the constructor private to enforce going through the right parsing api
// with schema checks. Make it public to allow parsing without full schema checks.
// (The checks will then occurr on calling .flatten)
case class MyType private(source: Map[String, Any]) extends Parsed[MyType] {
 val foo = parse(schema.foo) // String
 val bar = parse(schema.bar) // Option[Int]
 def schema = MyType
 
 // You can put constraints either in the schema fields, or here directly
 require(bar.forall(_ > 0), "bar must be positive")
}

val instance = MyType.parse(data) // returns object of type MyType

```


### Producing dynamic data

```scala

val data = Map("a" -> "lalala", "b" -> 123, "extra_data" -> Seq(2,3,4))

// The same as above
object MyType ..
case class MyType ..

val instance = MyType.parse(data) // returns object of type MyType
val dataBack = instance.flatten // Map[String, Any] - Including unspecified fields in the source + specified default values if any were used

// assuming we haven't added anything through e.g. default values:
assert(data == databack)


```


### Nested types

```scala

object MyRoot extends Schema[MyRoot] {
 val foo = required[Map[String, Seq[MyInner]]]("foo")
}
case class MyRoot private(root: Map[String, Any]) extends Parsed[MyRoot] {
 val foo = parse(schema.foo)
 def schema = MyRoot
}

// Declaring your other class in the same way ..
object MyInner ..
case class MyInner ..

```

### 'Constructors'

While writing tests or if you wish to use Parsed subclasses as your application's inner types, writing maps and then parsing them can be rather cumbersome. The best way to get around this is to add some constructors. Either the traditional way directly to your classes or with the pattern seen below on their schemas (=companion objects).

We can extend our previous example type MyType with such a Schema constructor:

```scala

object MyType extends Schema[MyType] {
 val foo = required[String]("a", default = "foo_default") // Creates a Field of type String
 val bar = optional[Int]("b") // Creates a Field of type Int
 
 def apply(foo: String, 
           bar: Option[Int] = None, 
           extaData: Map[String, Any] = Map.empty): MyType = marshal (
   this.foo -> foo,
   this.bar -> bar,
   extraData
  )
}

```

See classes 'MapData' and 'Parsed' for more information on this API.


### Type composition

Documentation; WIP. See tests


### Type migration

Each Heisenberg schema comes with an automatically created parser (Map[String, Any] => MyObjectType) - This parser is called when you call .parse(..) - as in the examples above. 

It is possible to override this parser field in your Schemas. For migration purposes this can be a parser which handles more than one object type - i.e. a parser that first tries to parse => MyNewObjectType, and if that fails, tries to parse => MyOldObjectType and transforms the result to a MyNewObjectType. For this to work we need:
* MyNewObjectType.defaultParser
* MyOldObjectType.defaultParser
* Your data transformation, 'migrator': [MyOldObjectType] => [MyNewObjectType]

We then simply override the 'parser' field in our Schema, e.g:

```scala

object MyNewObjectType extends Schema[MyNewObjectType] {
 def myMigration(old: MyOldObjectType): MyNewObjectType = {
  .. // Your custom migration code : 
  .. // MyOldObjectType => MyNewObjectType (See tests for examples)
 }
 override val parser = Migration.parser(MyNewObjectType.defaultParser, MyOldObjectType.defaultParser, myMigration)
}

```

Any old stored/received data to be atomatically migrated the next time it is parsed. If you don't want this automatic behaviour you can always do the above manually. This is of course only necessary if you make breaking changes to your model (e.g. change field names or semantics). 

Simple schema changes like adding and removing non-required fields is fine, as long as you don't reuse the same field name for another data type. Basically expect the same behaviour as your standard serialization libraries á la Protobuf/Thrift.


### Field migration

Documentation; WIP. See tests


### Custom types

```scala
object Event extends Schema[Event] {
 // to handle custom types, we must provide a producer and a parser. 
 // Not doing so will cause the compilation to fail. (here: Instant = java.time.Instant)
 implicit val iProducer = (t: Instant) => i.toString // Or expose an instance of the MapDataProducer[Instant] trait instead
 implicit val iParser = (x: Any) => x match { // Or expose an instance of the MapDataParser[Instant] trait instead
  case x: String => Instant.parse(x) // iso timestamp string
  case x: Number => new Instant(x, 0) // epoch millis
  case x => throw MapDataParser.WrongType(expect = "String or Number for Instant", actual = x.getClass.toString)
 }

 val timeStamp = required[Instant]("log_time", default = Instant.now())
 val content = optional[String]("content")
}
case class Event private(root: Map[String, Any]) extends Parsed[Event] {
 val timeStamp = parse(schema.timeStamp)
 val content = parse(schema.content)
 def schema = Event
}

```

## Default supported types

As seen in the examples above - you need to provide a parser and producer ([type classes](https://twitter.github.io/scala_school/advanced-types.html) - implementing the traits MapDataProducer[T] and MapDataParser[T]) for every Custom type not supported by default. The following types are supported by default:
* fixed point numbers (Byte, Short, Int, Long, BigInt)
* floating point numbers (Float, Double)
* String
* Boolean
* Seq[T : MapDataProducer : MapDataParser]
* Map[String, T : MapDataProducer : MapDataParser]
* Set[T : MapDataProducer : MapDataParser]
* Either[L: MapDataProducer : MapDataParser, R : MapDataProducer : MapDataParser]
* Subclasses of Parsed (as in the examples above)

Parsers are instantiated at application load (As implicit Field parameters when your schemas are loaded by the classloader) and verified to exist in compile time. If you are missing one for your type - you will know when you compile your code.


## Etc

Heisenberg is built using [type classes](https://twitter.github.io/scala_school/advanced-types.html) which specify how each type should be observed. Parsed objects are always accompanied by their source data (on any level of nesting), so no information is lost even though we only specify a subset of all actual fields.

Want to consolidate data of different models into a single one? Got data mixed from different application versions? Want to support multiple client application versions? Building a simple object persistence layer or routing service where only a subset of information needs to be parsed - *but the source data still needs to be kept intact*? - Why not give Heisenberg a chance :).

Got some FancyType that you don't want to rewrite for Heisenberg type but still want to mix in? Just provide a MapDataProducer[FancyType] and MapDataParser[FancyType] [type class](https://twitter.github.io/scala_school/advanced-types.html) and you're good to go!

The name Heisenberg comes from uncertainty in the data, but also wanting to observe as little of the data as possible, so as to not constrain whoever has the real model/definition of the data and let them evolve their model freely. 

There's no code generation in Heisenberg, but if you need it, it shouldn't be too hard to add in both directions (e.g. protobuf <-> heisenberg, heisenberg <-> cql statements etc.. You can probably do it on-the-fly, dynamically ;) )

May at some point leverage Shapeless and/or Scalaz, but for now is just plain-old-scala.
