# Heisenberg
A scala library for handling dynamic and evolving [key,value] data when you're uncertain about its model, if there is no model, or you just don't care. - Get the best of both dynamic and static worlds.

* Heisenberg is very much WIP.. Examples & doc may come later.
  * All suggestions appreciated

## Heisenberg helps with (w/o data loss)
* Persistence of partially defined and evolving data
* Routing of partially defined and evolving data
* Statically typed manipulation of dynamic data
* Migration & consolidation of dynamic data
* Validation of dynamic data


## Key features
* Selective parsing/validation without truncating source data
* Mapping [key,value] <-> [your class] without loss of information
* Support custom types & representations
* Support fields of inconsistent types (e.g. x.a sometimes int, sometimes string)
* Automatic data model migration (field name changes, semantic changes etc.)
* Type classes instead of reflection
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

case class MyType(source: Map[String, Any]) extends Parsed[MyType] {
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
val dataBack = instance.flatten // Map[String, Any] - Including default values if any were used

// assuming we haven't added anything through e.g. default values:
assert(data == databack)


```


## Etc

Heisenberg is built using type classes which specify how each type should be observed. Parsed objects are always accompanied by their source data (on any level of nesting), so no information is lost even though we only specify a subset of all actual fields.

Want to consolidate data of different models into a single one? Got data mixed from different application versions? Want to support multiple client application versions? Building a simple object persistence layer or routing service where only a subset of information needs to be parsed - *but the source data still needs to be kept intact*? - Why not give Heisenberg a chance :).

Got some FancyType that you don't want to rewrite for Heisenberg type but still want to mix in? Just provide a MapDataProducer[FancyType] and MapDataParser[FancyType] type class and you're good to go!

The name Heisenberg comes from uncertainty in the data, but also wanting to observe as little of the data as possible, so as to not constrain whoever has the real model/definition of the data and let them evolve their model freely. 

There's no code generation in Heisenberg, but if you need it, it shouldn't be too hard to add in both directions (e.g. protobuf <-> heisenberg, heisenberg <-> cql statements etc.. You can probably do it on-the-fly, dynamically ;) )

May at some point leverage Shapeless and/or Scalaz, but for now is just plain-old-scala.
