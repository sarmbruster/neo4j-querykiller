neo4j-querykiller
=================

This project's goal is to provide a convenient way to terminate individual cypher queries running on a Neo4j server.

> NB: for now, only the old, non-transactional cypher endpoint is supported.

Installation
------------

The simple way:

Download Neo4j from the [downloads page](http://www.neo4j.org/download) and extract it. Then run the following:

    ./gradlew -Pneo4jDirectory=<neo4j-dir> deploy

Gradle's deploy target copies the querykiller jar file to your Neo4j folder and sets up the configuration for you. In detail, the following actions are taken:
* build the jar file for querykiller and copy it to `<neo4j-dir>/plugins`
* amend `execution_guard_enabled=true` to `<neo4j-dir>/conf/neo4j.properties`
* amend `org.neo4j.server.thirdparty_jaxrs_classes=org.neo4j.extension.querykiller=/querykiller` to `<neo4j-dir>/conf/neo4j-server.properties`

Features
--------

### list running queries

neo4j-querykiller exposes a list of the currently running cypher queries using a HTTP to http://localhost:7474/querykiller. This returns a JSON array with a hashmap for each running query containing the following keys:

* `cypher`: cypher statement
* `key`: key to be used for killing this query
* `started`: timestamp when query execution has started
* `thread`: thread being used for that query

### killing a query

To terminate a query send a HTTP DELETE to http://localhost:7474/querykiller/<key> where `<key>` is one of the keys from the query list above. If termination is successfull, you'll get back a 204 return status.

### shell extension

Within neo4j-shell a new command is available:

    `query`: lists all queries

    `query -k <key>`: kills the query identified by <key>.

An example
----------

Create long running query:

    curl -X POST -H Accept:application/json -H Content-Type:application/json -d '{"query": "MATCH (a)-[r*]-(c) RETURN a"}' -v  http://localhost:7474/db/data/cypher

Check which queries are running:

    curl http://localhost:7474/querykiller/
    [{"cypher":"MATCH (a)-[r*]-(c) RETURN a","endPoint":"/cypher","thread":92,"since":3847,"key":"2161824329","remoteUser":null,"remoteHost":"127.0.0.1"}]

Kill the query by using the 'key' value from the previous query:

    curl -X DELETE http://localhost:7474/querykiller/2161824329

Implementation
--------------

QueryRegistryExtension is a kernel extension to Neo4j and allows to register and unregister queries. Upon registering a VetoGuard is established and a identifier for that query is created. When a query should be terminated, QueryRegistryExtension#abortQuery sets a flag in the respective guard. The next graph access will result the VetoGuard to emit an exception which causes the query to stop.

A servlet filter (QueryKillerFilter.java) is registered via a SPIPPluginLifecycle: Lifecycle.java. The filter intercepts every execution of a cypher statement and registers/unregisters it with the QueryRegistry.

A REST endpoint (QueryKillerService) exposes the contents of the registry and therefore the user can see which queries are currently running. Using the query's key and a HTTP delete request any running query can be terminated.

QueryKillerApp contributes a interface for listing running queries and termination of them to the shell.

further ideas
-------------

* gather statistics of queries
* integration in Neo4j browser
* support for transactional cypher endpoint
* expose querykiller as a JMX bean
* add tests for shell extension
* better docs
