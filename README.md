# Heisenberg
A scala library for dynamic [key,value] data - Get the best of both dynamic and static typing

Heisenberg helps solve, without loss of information, the problems of
* Persistence of partially defined and evolving data
* Routing of partially defined and evolving data
* Data migration and versioning
* Data model consolidation
* Data validation

Heisenberg provides
* Selective parsing/validation without modifying/dropping the source data
* Mapping of [key,value] <-> POJO without loss of information
* Support for custom types & representations
* Automatic data model migration (field name changes, semantic changes etc.)
* Type classes instead of reflection
* No code generation (just pure Scala)
* Custom arbitrary data validation (self-contained)
* Simple API

Heisenberg works well with dynamic data format, such as
* Json (e.g. json4s, ..)
* Schemaless NoSql databases (e.g. mongodb, couchbase, ..)
* Any Key-Value store, really.. 
  * currently anything that can be viewed as Map[String, Any]

Heisenberg is built using type classes which specify how each type should be observed. Parsed objects are always accompanied by their source data (on any level of nesting), so no information is lost even though we only specify a subset of all actual fields.

Building a simple object persistence layer or routing service where only a subset of information needs to be parsed - *but everything still needs to be kept intact*? - No problem - Just specify those fields you're interested in analyzing.

Got some FancyType that you don't want to rewrite to a Heisenberg type but still want to mix in? Just provide a MapDataProducer[FancyType] and MapDataParser[FancyType] type class and you're good to go!

The name Heisenberg comes from wanting to observe as little of the data as possible, so as to not constrain whoever has the real model/definition of the data and let them evolve their model freely. 
