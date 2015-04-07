neo4j-querykiller
=================

This project's main goal is to provide a convenient way to terminate individual cypher queries running on a Neo4j server. A secondary goal is to provide statistics on how long and how often a certain query is executes. This information basically gives the most impacting queries as candidates for optimization.

Installation
------------

Use [gradle](http://www.gradle.org) to build the project:

    ./graldew fatJar
    
The `fatJar` task creates one single jar file containing the code of neo4j-querykiller itself and those of its dependencies not being part of the neo4j distribution. See [build.gradle](build.gradle)'s the `fat` dependencies in the dependency section.
     
Copy (or symlink) the resulting file `./build/libs/neo4j-querykiller-all-1.0.0-SNAPSHOT.jar` to Neo4j's plugin folder.
  
Change configuration in `$NEO4J/conf/neo4j-server.properties`:

    org.neo4j.server.thirdparty_jaxrs_classes=org.neo4j.extension.querykiller.server=/querykiller,org.neo4j.extension.querykiller.statistics=/statistics

After a restart using `$NEO4J_HOME/bin/neo4j restart` the extensions are active.

Features
--------

### list running queries

neo4j-querykiller exposes a list of the currently running cypher queries using a HTTP to http://localhost:7474/querykiller. This returns a JSON array with a hashmap for each running query containing the following keys:

* `cypher`: cypher statement
* `key`: key to be used for killing this query
* `started`: timestamp when query execution has started
* `thread`: thread being used for that query

### killing a query

To terminate a query send a HTTP DELETE to http://localhost:7474/querykiller/<key> where `<key>` is one of the keys from the query list above. If termination is successful, you'll get back a 204 return status.

### shell extension

Within neo4j-shell a new command is available:

`query`: lists all queries

`query -k <key>`: kills the query identified by &lt;key&gt;.

An example
----------

Create long running query:

    curl -X POST -H Accept:application/json -H Content-Type:application/json -d '{"query": "MATCH (a)-[r*]-(c) RETURN a"}' -v  http://localhost:7474/db/data/cypher

Check which queries are running:

    curl http://localhost:7474/querykiller/
    [{"cypher":"MATCH (a)-[r*]-(c) RETURN a","endPoint":"/cypher","thread":92,"since":3847,"key":"2161824329","remoteUser":null,"remoteHost":"127.0.0.1"}]

Kill the query by using the 'key' value from the previous query:

    curl -X DELETE http://localhost:7474/querykiller/2161824329
    
### statistics
    
Querykiller implements the observer pattern. One observer is statistics.

Get a list of all queries run so far:

    curl http://localhost:7474/statistics/

    {
        "[\"MATCH (n:Person) RETURN n LIMIT 25\"]": {
            "durations": {
                "2015-04-04T12:11:57.358+0000": 1200, 
                "2015-04-04T12:12:05.929+0000": 7
            }, 
            "total": 1207
        }, 
        "[\"MATCH a -[r]- b WHERE id(a) IN[0,6,7,8]\\nAND id(b) IN[0,6,7,8]\\nRETURN r;\"]": {
            "durations": {
                "2015-04-04T12:11:58.588+0000": 417, 
                "2015-04-04T12:12:06.005+0000": 8
            }, 
            "total": 425
        }
    }
    
A map is returned. Its keys are the cypher queries, the values are a map holding the aggregated total runtime of this query ("total")
    and a collection of the individual invocations with timestamp and duration.
    
NB: the statistics can grow large and memory consuming, to clear them:

    curl -X DELETE http://localhost:7474/statistics/
    
Implementation
--------------

QueryRegistryExtension is a kernel extension to Neo4j and allows to register and unregister queries. The registration of a query requires providing the transaction it runs in. For the legacy cypher endpoint a transaction is provided by the servlet filter (see below). For the transactional cypher endpoint the respective transaction is retrieved via Neo4j's internal `TransactionRegistry`. The kernel extension features termination of a query through sending `terminate()` to its transaction.
   
The kernel extension additionally acts as [Observable](https://docs.oracle.com/javase/7/docs/api/java/util/Observable.html). Other components (e.g. QueryStatisticsExtension) can register themselves as observer. See [here](./src/main/java/org/neo4j/extension/querykiller/events) for a list of supported events.

A servlet filter (QueryKillerFilter.java) is registered via a SPIPPluginLifecycle: Lifecycle.java. The filter intercepts every execution of a cypher statement and registers/unregisters it with the QueryRegistry.

A REST endpoint (QueryKillerService) exposes the contents of the registry and therefore the user can see which queries are currently running. Using the query's key and a HTTP delete request any running query can be terminated.

QueryKillerApp contributes a interface for listing running queries and termination of them to the shell.

further ideas
-------------

* [x] gather statistics of queries
* [ ] integration in Neo4j browser
* [x] support for transactional cypher endpoint
* [x] expose querykiller as a JMX bean
* [ ] add tests for shell extension
* [ ] better docs
* [x] make tests more robust (use events instead of static waiting pauses)
