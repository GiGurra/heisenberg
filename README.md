# Heisenberg
A scala library for dynamic data

Heisenberg provides 
* Selective parsing/validation without modifying/dropping the source data
* Read any data source viewable as Map[String, Any] to a thin POJO
* Write objects back to Map[String, Any] without loss of information
* Support for custom field types 
* Automatic data model migration (field name changes, semantic changes etc.)
* Type classes instead of relection
* Simple API

Heisenberg is built using type classes which specify how each field should be observed. Parsed objects are always accompanied by their source data (on any level of nesting), so no information is lost even though we only specify a subset of all actual fields.

Building a simple object persistence layer or routing service where only a subset of information needs to be parsed? 
No problem - Just specify those fields you're interested in. 

The name Heisenberg comes from wanting to observe as little of the data as possible, so as to not constrain whoever has the real model/definition of the data and let them evolve their model freely. 
