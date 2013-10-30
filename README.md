neo4j-querykiller
=================

This project's goal is to provide a convenient way to terminate individual cypher queries running on a Neo4j server.

Installation
------------

The simple way:

* download Neo4j distribution and extract it
* call `./gradlew -Pneo4jDirectory=<neo4j-dir> deploy

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

To terminate a query send a HTTP DELETE to http://localhost:7474/querykiller/<key> where `<key>` is one of the keys from the query list above.


Implementation
--------------

A servlet filter (QueryKillerFilter.java) is registered via a SPIPPluginLifecycle: Lifecycle.java. The filter intercepts every execution of a cypher statement. The query is first registered in a global QueryRegistry and gets assigned a unique key. After execution the query is removed from the registry. Query registration encompasses setting up a guard for the current thread. A guard is a simple interface having a `check()` method. During query processing the guard's `check()` is invoked upon every load operation of a node or relationship. The default guard implementations use either a maximum limit of number of nodes or a maximum duration as termination criteria. The guard implementation used here (VetoGuard) is based on a simple boolean flag that can be switched by a REST call.

A REST endpoint (QueryKillerService) exposes the contents of the registry and therefore the user can see which queries are currently running. Using the query's key and a HTTP delete request any running query can be terminated.

further ideas
-------------

* shell extension for querykiller: list queries and kill them
* better docs
* gather statistics of queries
* integration in Neo4j browser
* add more data in query list: http agent, source ip
* support for transactional cypher endpoint
