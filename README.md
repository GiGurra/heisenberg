# Heisenberg
A scala library for dynamic [key,value] data - Get the best of both dynamic and static typing

* Heisenberg is very much WIP.. Examples & doc may come later.
  * All suggestions appreciated


## Heisenberg helps solve (without data loss)
* Persistence of partially defined and evolving data
* Routing of partially defined and evolving data
* Statically typed manipulation of dynamic data
* Migration & consolidation of dynamic data
* Validation of dynamic data


## Key features
* Selective parsing/validation without truncating source data
* Mapping of [key,value] <-> POJO without loss of information
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


## Etc

Heisenberg is built using type classes which specify how each type should be observed. Parsed objects are always accompanied by their source data (on any level of nesting), so no information is lost even though we only specify a subset of all actual fields.

Want to consolidate data of different models into a single one? Got data mixed from different application versions? Want to support multiple client application versions? Building a simple object persistence layer or routing service where only a subset of information needs to be parsed - *but the source data still needs to be kept intact*? - Why not give Heisenberg a chance :).

Got some FancyType that you don't want to rewrite for Heisenberg type but still want to mix in? Just provide a MapDataProducer[FancyType] and MapDataParser[FancyType] type class and you're good to go!

The name Heisenberg comes from uncertainty in the data, but also wanting to observe as little of the data as possible, so as to not constrain whoever has the real model/definition of the data and let them evolve their model freely. 

There's no code generation in Heisenberg, but if you need it, it shouldn't be too hard to add in both directions (e.g. protobuf <-> heisenberg, heisenberg <-> cql statements etc.. You can probably do it on-the-fly, dynamically ;) )

May at some point leverage Shapeless and/or Scalaz, but for now is just plain-old-scala.
