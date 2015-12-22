# Heisenberg
A scala library for dynamic data

Heisenberg solves, without loss of information, the problems of
* Persistence of partially defined data types
* Routing of partially defined data types
* Data migration and versioning
* Data model consolidation

Heisenberg provides
* Selective parsing/validation without modifying/dropping the source data
* Mapping of Map[String, Any] <-> POJO without loss of information
* Support for custom field types
* Automatic data model migration (field name changes, semantic changes etc.)
* Type classes instead of relection
* Simple API

Heisenberg is built using type classes which specify how each field should be observed. Parsed objects are always accompanied by their source data (on any level of nesting), so no information is lost even though we only specify a subset of all actual fields.

Building a simple object persistence layer or routing service where only a subset of information needs to be parsed? 
No problem - Just specify those fields you're interested in. 

Got some FancyType class that you don't want to convert to a Heisenberg type but still want to have in some of your Heisenberg fields? Just provide a MapDataProducer[FancyType] and MapDataParser[FancyType] and you're good to go!

The name Heisenberg comes from wanting to observe as little of the data as possible, so as to not constrain whoever has the real model/definition of the data and let them evolve their model freely. 
