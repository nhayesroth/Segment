## Segment - Redis Proxy Server

### Overview
This project includes code to run a proxy server on top of a backing Redis instance. It allows clients to request String values from the backing Redis instance via HTTP & RESP. The server adds basic LRU caching functionality for recently retrieved key-value pairs.

### Instructions to run
#### Single-click build and test
1. Download or clone this repository
2. cd into it
`cd Segment`
3. Run the make target
`make test`

#### Configuration
The proxy server can be configured by passing environment variables to the `make test` command via `-e`.

For example, the following command would run the application with the maximum number of keys that the proxy can cache changed to 20:
`make test -e CACHE_CAPACITY=20`

For a full list of configurable variables, see: 
* [docker-compose.yml](https://github.com/nhayes-roth/RedisProxy/blob/master/docker-compose.yml)
* [Configuration.java](https://github.com/nhayes-roth/RedisProxy/blob/master/RedisProxy/src/main/java/configuration/Configuration.java)

### How it works
Running the `make test` target will cause Docker to produce 3 different services: redis, redis_proxy, integration_tests.
* redis: standard redis container
* redis_proxy: this application, which adds caching to the redis instance
* integration_tests: test container which runs a suite of tests against the configuration of redis & redis_proxy
 * This test service actually depends on the source code for the proxy.
 * The proxy's code is deployed to Maven at https://github.com/nhayes-roth/mvn-repo/.
 * The test service downloads the packaged jar during its own build phase.

## Architecture

### Tools
#### [Docker & Docker Compose](https://www.docker.com/)
Used to deploy the application code.
#### [Apache Maven](https://maven.apache.org/)
Used to pull in Java dependencies and run the integration test suite.
#### [Lettuce.io](https://lettuce.io/core/release/reference/)
Lettuce is a scalable thread-safe Redis client based on netty and Reactor. Lettuce provides asynchronous APIs to interact with redis, which allows this application to process multiple requests without waiting for in-flight request(s) to complete.

### Primary Classes
#### [Server](https://github.com/nhayes-roth/RedisProxy/blob/master/RedisProxy/src/main/java/server/Server.java)
The Server is the program's main class, which starts the application. It reads environment variables set in Configuration, initializes the LruCache, and spawns two separate threads to handle HTTP and RESP connections.

#### [[Http](https://github.com/nhayes-roth/RedisProxy/blob/master/RedisProxy/src/main/java/http/HttpServer.java "Http")|[Resp](https://github.com/nhayes-roth/RedisProxy/blob/master/RedisProxy/src/main/java/resp/RespServer.java "Resp")]Server
These two classes run as independent threads. Each thread listens to a separate port for connections and manages its own threadpool to handle concurrent requests in parallel.

#### [[Http](https://github.com/nhayes-roth/RedisProxy/blob/master/RedisProxy/src/main/java/http/HttpRequestHandler.java "Http")|[Resp](https://github.com/nhayes-roth/RedisProxy/blob/master/RedisProxy/src/main/java/resp/RespRequestHandler.java "Resp")]RequestHandler
These two classes run as independent threads, spawned by the corresponding [Http|Resp]Server class to handle a single request.

#### [LruCache](https://github.com/nhayes-roth/RedisProxy/blob/master/RedisProxy/src/main/java/configuration/Configuration.java)
A cache of key-value pairs that sits on top of the backing redis instance. The cache evicts least-recently-used entries based on two conditions:
* The key has been in the cache too long (expireAfterWrite)
* The number of keys in the cache exceeds its capacity (maximumSize)

This object is based on the com.google.common.cache.CacheBuilder and CacheLoader objects, which are thread-safe. If a request is made for a key that does not exist in the cache, the CacheLoader will check the backing redis instance.
##### Algorithmic Complexity
All the cache operations are O(1) in time and space.

#### [Configuration](https://github.com/nhayes-roth/RedisProxy/blob/master/RedisProxy/src/main/java/configuration/Configuration.java)
Reads system environment variables and constructs a value class that can be shared by various classes in the project.

## Time Taken
* Building/Deploying: O(days)
 * This was my first project using Docker and only my second using Maven. I spent more time than I care to admit reading about and debugging these systems. I had no context for Dockerfiles vs. docker-compose.yml, Docker networking, packaging Maven projects in docker, running Maven tests as integration tests (as opposed to running them at compile-time), etc.
* Server setup/multi-thread hiearchy: O(1hr)
* Redis/Lettuce: O(1hr)
* LruCache: O(20min)

## Requirements Not Met/What's Next?
* I have yet to implement the RESP protocol. I intended to, but spent far longer fighting with Docker/Maven than I expected.
* I have written 0 unit tests. If I were to deploy this application for real:
 * Each individual class would need its own set of unit tests
 * I would also add some EndToEnd tests that use in-memory Redis fakes to test behavior before deploying.
 * All of these tests would run during the packaging phase, in addition to the final integration test phase.