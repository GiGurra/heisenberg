# Heisenberg
A scala library for dynamic [key,value] data

Heisenberg helps solve, without loss of information, the problems of
* Persistence of partially defined and evolving data
* Routing of partially defined and evolving data
* Data migration and versioning
* Data model consolidation

Heisenberg provides
* Selective parsing/validation without modifying/dropping the source data
* Mapping of [key,value] <-> POJO without loss of information
* Support for custom types & representations
* Automatic data model migration (field name changes, semantic changes etc.)
* Type classes instead of relection
* No code generation (just pure Scala)
* Simple API

Heisenberg works well with dynamic data libraries, such as
* Xml 
* Json (e.g. json4s, ..)
* Schemaless NoSql databases (e.g. mongodb, couchbase, ..)
* Any Key-Value store, really.. 
  * currently anything that can be viewed as Map[String, Any]

Heisenberg is built using type classes which specify how each field should be observed. Parsed objects are always accompanied by their source data (on any level of nesting), so no information is lost even though we only specify a subset of all actual fields.

Building a simple object persistence layer or routing service where only a subset of information needs to be parsed? 
No problem - Just specify those fields you're interested in. 

Got some FancyType class that you don't want to convert to a Heisenberg type but still want to have in some of your Heisenberg fields? Just provide a MapDataProducer[FancyType] and MapDataParser[FancyType] and you're good to go!

The name Heisenberg comes from wanting to observe as little of the data as possible, so as to not constrain whoever has the real model/definition of the data and let them evolve their model freely. 
