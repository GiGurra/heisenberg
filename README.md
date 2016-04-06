# Heisenberg
A scala library for handling dynamic and evolving [key,value] data when you're uncertain about its model, if there is no model, or you just don't care.

## Heisenberg makes dynamic data easier to manage

Think of your standard data<->OO mapper, but with:

* Selective mapping without truncating source data
* Fields of inconsistent types (e.g. x.a sometimes int, sometimes string)
* Automatic data migration (field name changes, semantic changes etc.)
* [Type classes](https://twitter.github.io/scala_school/advanced-types.html) instead of reflection
* No macros, annotations or code generation (just pure Scala)
* Custom arbitrary data validation (self-contained)
* Seamless transitions between static and dynamic data
* Custom types & representations
* Simple API


## Compatibility
* Json (e.g. json4s, ..)
* Schemaless NoSql databases (e.g. mongodb, couchbase, ..)
* Any Key-Value data, really.. 
  * currently anything that can be viewed as Map[String, Any]


## Misc

* Used by [valhalla-game backend](https://github.com/saiaku-gaming/valhalla-server) (mongodb<->[finagle + franklin + heisenerg]<->unreal-engine)
  * This is where the heisenberg project started
* For more ideas on how to use Heisenberg check out:
  * [Franklin](https://github.com/GiGurra/franklin)
  * [Franklin-Heisenberg-bridge](https://github.com/GiGurra/franklin-heisenberg-bridge)
* Heisenberg is very much WIP.. Examples & doc pending.
  * All suggestions appreciated
  * Yet to decide on API design and what features should/not be included


## Examples

Below are some simple examples showing how to use Heisenberg


### Parsing dynamic data

```scala

val data = Map("a" -> "lalala", "b" -> 123, "extra_data" -> Seq(2,3,4))

object MyType extends Schema[MyType] {
 val foo = required[String]("a", default = "foo_default")
 val bar = optional[Int]("b")
}

case class MyType (source: Map[String, Any]) extends Parsed[MyType.type] {
 val foo: String = parse(schema.foo)
 val bar: Option[Int] = parse(schema.bar)
 
 // Example constraint
 require(bar.forall(_ > 0), "bar must be positive")
}

val instance: MyType = MyType.parse(data)

```


### Producing dynamic data

```scala

val data = Map("a" -> "lalala", "b" -> 123, "extra_data" -> Seq(2,3,4))

// The same as above
object MyType ..
case class MyType ..
val instance ..

// Including all the source data + default values for missing fields
val dataBack = instance.flatten // Map[String, Any]

// assuming we haven't added anything through e.g. default values:
assert(data == databack)


```


### Nested types

```scala

object MyRoot extends Schema[MyRoot] {
 val foo = required[Map[String, Seq[MyInner]]]("foo")
}
case class MyRoot (source: Map[String, Any]) extends Parsed[MyRoot.type] {
 val foo = parse(schema.foo)
}

// Declaring your other class in the same way ..
object MyInner ..
case class MyInner ..

```

### 'Constructors'

While writing tests or if you wish to use Parsed subclasses as your application's inner types, writing maps and then parsing them can be rather cumbersome. The best way to get around this is to add some constructors. Either the traditional way directly to your classes or with the pattern seen below on their schemas (=companion objects):

```scala

object MyType extends Schema[MyType] {
 val foo = required[String]("a", default = "foo_default") // Creates a Field of type String
 val bar = optional[Int]("b") // Creates a Field of type Int
 
 def apply(foo: String, 
           bar: Option[Int] = None, 
           extraData: Map[String, Any] = Map.empty): MyType = marshal (
   this.foo -> foo,
   this.bar -> bar,
   extraData
  )
}

```

See classes 'MapData' and 'Parsed' for more information on this API.


### Type composition

Suppose you wish to partially share capabilities, fields, constraints etc between different types while sticking to DRY. Heisenberg supports mixins of components and component schemas. Below is an example taken from an early version of the Valhalla Game model.

The Heisenberg types in this example are:

* Character (A character with owner stored in mongodb)
* SaveCharacter (A save character request received from unreal engine through finagle)

They are almost identical, except that the type 'Character' contains an additional field 'owner' (see below).

```scala


/////////////////////////////
// Our storage definition

case class Character (source: Map[String, Any])
  extends Parsed[Character.type]
  with CharacterData
  with CharacterOwner {
}

object Character
  extends Schema[Character]
  with CharacterDataSchema
  with CharacterOwnerSchema {

  def apply(name: String,
            owner: String,
            inventory: Seq[Item],
            itemSlots: ItemSlots,
            skills: Seq[Skill],
            mesh: Mesh) = marshal(
    this.name -> name,
    this.owner -> owner,
    this.inventory -> inventory,
    this.itemSlots -> itemSlots,
    this.skills -> skills,
    this.mesh -> mesh
  )
}


/////////////////////////////
// Our message definition

case class SaveCharacter (source: Map[String, Any])
  extends Parsed[SaveCharacter.type]
  with CharacterData {
}
object SaveCharacter
  extends Schema[SaveCharacter]
  with CharacterDataSchema


/////////////////////////////
// Our components and schemas

trait CharacterData extends Component {
  def schema: CharacterDataSchema
  val name = parse(schema.name)
  val inventory = parse(schema.inventory)
  val itemSlots = parse(schema.itemSlots)
  val skills = parse(schema.skills)
  val mesh = parse(schema.mesh)
}

trait CharacterOwner extends Component {
  def schema: CharacterOwnerSchema
  val owner = parse(schema.owner)
}

trait CharacterDataSchema extends ComponentSchema {
  val name = required[String]("name")
  val inventory = required[Seq[Item]]("inventory")
  val itemSlots = required[ItemSlots]("item_slots")
  val skills = required[Seq[Skill]]("skills")
  val mesh = required[Mesh]("mesh")
}

trait CharacterOwnerSchema extends ComponentSchema {
  val owner = required[String]("owner")
}


```

It's a matter of taste if you prefer mixing in components or just having the Character class above store a SaveCharacter object + an owner field. I prefer component mixins - Heisenberg will let you do either.

A word on traditional OO inheritance: 
* Heisenberg does not (yet) support inheritance
  * So stick to mixins/components for now
  * (although inheritance probably works with Heisenberg.. ;) it's not officially supported).


### Type migration

Each Heisenberg schema comes with an automatically created parser (Map[String, Any] => MyObjectType) - This parser is called when you call .parse(..) - as in the examples above. 

It is possible to override this parser field in your Schemas. For migration purposes this can be a parser which handles more than one object type - i.e. a parser that first tries to parse => MyNewObjectType, and if that fails, tries to parse => MyOldObjectType and transforms the result to a MyNewObjectType. For this to work we need:
* MyNewObjectType.defaultParser
* MyOldObjectType.parser
* Your data transformation, 'migrator': [MyOldObjectType] => [MyNewObjectType]

We then simply override the 'parser' field in our Schema, e.g:

```scala

object MyNewObjectType extends Schema[MyNewObjectType] {
 def myMigration(old: MyOldObjectType): MyNewObjectType = {
  .. // Your custom migration code : 
  .. // MyOldObjectType => MyNewObjectType (See tests for examples)
 }
 override val parser = Migration.parser(MyNewObjectType.defaultParser, MyOldObjectType.parser, myMigration)
}

```

Any old stored/received data to be atomatically migrated the next time it is parsed. If you don't want this automatic behaviour you can always do the above manually. This is of course only necessary if you make breaking changes to your model (e.g. change field names or semantics). 

Simple schema changes like adding and removing non-required fields don't require migration, as long as you don't reuse the same field name for another data type. Basically expect the same behaviour as your standard serialization libraries á la Protobuf/Thrift.


### Field migration

There are several approaches you can use for field migration. First though - is this really what you want? 
Field migration is like the poor man's replacement/hack for type migration. But if you want it anyway, you can do something like: 

```scala
object TestType extends Schema[TestType] {
  // Here we define both what the field is and what it used to be
  val field = required[Int]("foo")
  val oldField = required[String]("bar")
}

case class TestType(source: Map[String, Any]) extends Parsed[TestType.type] {
  // Here we define how to transition from the old definition to the new
  val foo = parse(schema.field, orElse = 2 * parse(schema.oldField).toInt)
}
```

Suppose instead that the field changed type and semantics, but not name. Then you could do something like:

```scala
object TestType extends Schema[TestType] {
  val foobar = required[Either[Int, String]]("foobar")
}

case class TestType(source: Map[String, Any]) extends Parsed[TestType.type] {
  val foobar : Int = parse(schema.foobar) match {
                      case Left(int) => int
                      case Right(string) => 2 * string.toInt
                     }
}
```

Or define two separate fields refering to the same field name, and apply the previous strategy.


### Custom types

Sometimes you will want add write non-heisenberg objects to your schemas - Heisenberg only requires a MapDataParser[T] and a MapDataProducer[T] to be available during compile time. As long as those two exist, you can add whatever types you want. Below is an example:

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
case class Event (source: Map[String, Any]) extends Parsed[Event.type] {
 val timeStamp = parse(schema.timeStamp)
 val content = parse(schema.content)
}

```

## Default supported types

As seen in the examples above - you need to provide a parser and producer ([type classes](https://twitter.github.io/scala_school/advanced-types.html) - implementing the traits MapDataProducer[T] and MapDataParser[T]) for every Custom type not supported by default. The following types (and any combination of them) are supported by default:
* fixed point numbers (Byte, Short, Int, Long, BigInt)
* floating point numbers (Float, Double)
* String
* Boolean
* Seq[T : MapDataProducer : MapDataParser]
* Map[String, T : MapDataProducer : MapDataParser]
* Set[T : MapDataProducer : MapDataParser]
* Either[L: MapDataProducer : MapDataParser, R : MapDataProducer : MapDataParser]
* Option[T : MapDataProducer : MapDataParser] 
  * As above or the optional[T] schema keyword
* Subclasses of Parsed (as in the examples above)

Parsers are instantiated at application load (As implicit Field parameters when your schemas are loaded by the classloader) and verified to exist in compile time. If you are missing one for your type - you will know when you compile your code.

You can always override the default parser on both field and type level.


## What about performance?

* There's a lot of metadata here..
  * I know - Heisenberg is young. We'll make it faster if it turns out to be slow

* Hey - you said no reflection!
  * Heisenberg uses TypeTags to produce error messages. But it shouldn't impact happy path performance.


## Try it

Put a project dependency in your build.sbt. At some point I will push it to sonatype/maven central as well.

```sbt

.dependsOn(uri("https://github.com/GiGurra/heisenberg.git#0.2.6"))

```


## Etc

Heisenberg is built using [type classes](https://twitter.github.io/scala_school/advanced-types.html) which specify how each type should be observed. Parsed objects are always accompanied by their source data (on any level of nesting), so no information is lost even though we only specify a subset of all actual fields.

Want to consolidate data of different models into a single one? Got data mixed from different application versions? Want to support multiple client application versions? Building a simple object persistence layer or routing service where only a subset of information needs to be parsed - *but the source data still needs to be kept intact*? - Why not give Heisenberg a chance :).

Got some FancyType that you don't want to rewrite for Heisenberg type but still want to mix in? Just provide a MapDataProducer[FancyType] and MapDataParser[FancyType] [type class](https://twitter.github.io/scala_school/advanced-types.html) and you're good to go!

The name Heisenberg comes from uncertainty in the data, but also wanting to observe as little of the data as possible, so as to not constrain whoever has the real model/definition of the data and let them evolve their model freely. 

There's no code generation in Heisenberg, but if you need it, it shouldn't be too hard to add in both directions (e.g. protobuf <-> heisenberg, heisenberg <-> cql statements etc.. You can probably do it on-the-fly, dynamically ;) )

May at some point leverage Shapeless and/or Scalaz, but for now is just plain-old-scala.
