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

The above commands will build 2 java programs (the proxy server itself, and an integration test suite), setup a Redis instance and the proxy in docker containers, and run the integration test suite against the configuration.

#### How do I hit it?
After running `make test`, the proxy server should be up and running on your local machine. Assuming you didn't change change any of the REDIS_PORT, HTTP_PORT, or RESP_PORT environment variables, you can now hit the proxy on your local machine:

* Set a variable in the backing Redis instance:
 ```
 $ (printf "SET foo bar\r\n"; sleep 1) | nc localhost 6379
 +OK
 ```
* Retrieve the variable via HTTP:
 ```
 $ curl "localhost:8080/foo" -v
 *   Trying ::1...
 * TCP_NODELAY set
 * Connected to localhost (::1) port 8080 (#0)
 > GET /foo HTTP/1.1
 > Host: localhost:8080
 > User-Agent: curl/7.54.0
 > Accept: */*
 > 
 < HTTP/1.1 200 OK
 * no chunk, no close, no size. Assume close to signal end
 < 
 * Closing connection 0
 bar
 ```
* Retrieve a variable via RESP:
 ```
 $ (printf "GET foo\r\n"; sleep 1) | nc localhost 6379
 $3
 bar
 ```
#### Configuration
The proxy server can be configured by passing environment variables to the `make test` command via `-e`.

For example, the following command would run the application with the maximum number of cached keys raised to 20:
`make test -e CACHE_CAPACITY=20`

For a full list of configurable variables, see: 
* [Configuration.java](https://github.com/nhayes-roth/Segment/blob/master/RedisProxy/src/main/java/configuration/Configuration.java)

### How it works
Running the `make test` target will cause Docker to produce 3 different services: redis, redis_proxy, integration_tests.
* redis: standard Redis container.
* redis_proxy: the proxy server application, which adds caching to the Redis instance.
* integration_tests: test container which runs a suite of tests against the configuration of Redis & redis_proxy.
 * This test service actually depends on some of the source code for the proxy.
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
#### [Server](https://github.com/nhayes-roth/Segment/blob/master/RedisProxy/src/main/java/server/Server.java)
The Server is the program's main class, which starts the application. It reads environment variables set in Configuration, initializes the LruCache, and spawns two separate threads to handle HTTP and RESP connections.

#### [[Http](https://github.com/nhayes-roth/Segment/blob/master/RedisProxy/src/main/java/http/HttpServer.java "Http")|[Resp](https://github.com/nhayes-roth/Segment/blob/master/RedisProxy/src/main/java/resp/RespServer.java "Resp")]Server
* These two classes run as independent threads.
* Each thread listens to a separate port for connections and manages its own threadpool to handle concurrent requests in parallel.

#### [[Http](https://github.com/nhayes-roth/Segment/blob/master/RedisProxy/src/main/java/http/HttpRequestHandler.java "Http")|[Resp](https://github.com/nhayes-roth/Segment/blob/master/RedisProxy/src/main/java/resp/RespRequestHandler.java "Resp")]RequestHandler
* These two classes run as independent threads, spawned by the corresponding [Http|Resp]Server class to handle a single request.
* Each RespRequestHandler actually manages its own threadpool, which is used to handle pipelined RESP requests in parallel.

#### [LruCache](https://github.com/nhayes-roth/Segment/blob/master/RedisProxy/src/main/java/configuration/Configuration.java)
A cache of key-value pairs that sits on top of the backing Redis instance. The cache evicts least-recently-used entries based on two conditions:
* The key has been in the cache too long (expireAfterWrite)
* The number of keys in the cache exceeds its capacity (maximumSize)

This object is based on the com.google.common.cache.CacheBuilder and CacheLoader objects, which are thread-safe. If a request is made for a key that does not exist in the cache, the CacheLoader will check the backing Redis instance.

##### Algorithmic Complexity
* All the cache operations are O(1) in time and space.
* The size of the cache is linear up to its maximum capacity, though that is also capped based on the value configured at startup.

#### [Configuration](https://github.com/nhayes-roth/Segment/blob/master/RedisProxy/src/main/java/configuration/Configuration.java)
Reads system environment variables and constructs a value class that can be shared by various classes in the project.

## Time Taken
* Building/Deploying: O(days)
 * This was my first project using Docker and only my second using Maven. I spent more time than I care to admit reading about and debugging these systems. I had no context for Dockerfiles vs. docker-compose.yml, Docker networking, packaging Maven projects in docker, running Maven tests as integration tests (as opposed to running them at compile-time), etc.
* Server setup/multi-thread hiearchy: O(1hr)
* Redis/Lettuce: O(1hr)
* LruCache: O(20min)
* RESP protocol: O(1 day)
 * Most of this time was dedicated to testing/debugging corner cases.

## Requirements Not Met/What's Next?
* All requirements have been met.
* If I were to deploy this for real:
 * I would expand the set of unit tests (e.g. for classes such as the Servers, RequestHandlers, etc.).
 * I would refactor some of the RESP protocol handling. It is not very clean, currently.
 * I would refactor the E2E vs Integration tests. They are currently largely identical, but run at different stages of deployment.
 * I would refactor the ProxyServer package to allow the ProxyServerIntegrationTests package to depend on a smaller subset.
