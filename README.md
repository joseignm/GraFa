# GraFa
Faceted Browsing over Wikidata triples

## Use the system

The system is currently running on: http://grafa.dcc.uchile.cl

## Installation

### Required libraries:

* Lucene 6.5
* Tomcat 7
* RDF4J 2.2

### Config file

The file facet.properties contains the following options:

* languages: list of supported languages (using the same language tag from the dataset)
* entityIRI: prefix for entities
* propertyIRI: prefix for properties
* labelIRI: predicate of the triple containing the label
* descriptionIRI: predicate of the triple containing the description
* alt_labelIRI: predicate of the triples containing alt labels or aliases
* instanceOf: IRI (with no prefix) of the type or instance of predicate
* image: IRI (with no prefix) of the image predicate
* entityPrefix: prefix of all entities (excluding the domain prefix)

### Binaries

The included build.xml generates several jar files. These are the most important:

* index.jar: Creates an index from all entities based on an NT file
* rank.jar: Computes the graph and the Page Rank value of each entity
* boosts.jar: Copies the index from index.jar but adding the ranks from rank.jar
* cache.jar: Generates a list of all queries with a large results set that need caching.
* instances.jar: Creates an index of all instances/types of the main index and also caches the results from the list of cache.jar
* values.jar: Creates the cache for values for every property of every query that needs caching. This process may take a couple of days.
* grafa.war: Tomcat Webapp's war file. The config file need the directory of the indexes.
* The other jar files are for debugging and generate statistics (there may also be unused classes)
