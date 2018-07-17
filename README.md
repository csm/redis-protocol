Note, this has been hacked up to support a newer version of netty 4,
but only the netty4-server and related projects.

Available on clojars:

* [com.github.csm.redis/redis](https://clojars.org/com.github.csm.redis/redis)
* [com.github.csm.redis/util](https://clojars.org/com.github.csm.redis/util)
* [com.github.csm.redis/protocol](https://clojars.org/com.github.csm.redis/protocol)
* [com.github.csm.redis/netty4](https://clojars.org/com.github.csm.redis/netty4)
* [com.github.csm.redis/netty4-server](https://clojars.org/com.github.csm.redis/netty4-server)

The general idea is to make redis available for use within testing code, without resorting to forking off `redis-server`
or having it running already.

Original README contents below:

```
A very fast Redis client for the JVM.

Description of each module:

redisgen/
  Scrapes the redis.io/commands page and produce various typed clients and servers, very extensible

util/
  Some common encoding and data structures
client/
  Leverages the protocol module for encoding and decoding. Supports both synchronous
  and asynchronous pipelined requests from the RedisClient. Supports 2.6 commands.
protocol/
  Redis protocol encoder / decoder based on input/outputstreams. This is the
  fastest implementation if blocking i/o is ok for your use case.
benchmark/
  A redis-benchmark clone that uses this Java client for comparison testing.

netty/
  A netty 3.5.X compatible codecs for building Redis clients
netty-client/
  Complete client except for MULTI/EXEC.

netty4/
  A netty 4.0.0.Alpha1 compatible codec for building Redis clients
netty4-server/
  A very high performance in-JVM memory redis server clone

util/
  Some library functions used by both the blocking client and the netty clients

In the experiments branch you can find:
  - finagle
  - vertx
  - NIO bytebuffers
  - HBase Loader
  - and more!

Maven dependency:

    <dependency>
      <groupId>com.github.csm.redis</groupId>
      <artifactId>client</artifactId>
      <version>0.6</version>
    </dependency>

Benchmarks

Various redis client benchmarks

- JDK 7u6
- redis-server 2.4.4
- for (i <- 0 to 1,000,000) { set(i, "value") }
- Conditions
  - localhost
  - 1 connection
  - request and wait for response
- Results
  - Finagle-Redis 5.3.1-SNAPSHOT
    - finagle, netty 3, naggati codec
    - 3.26 MB/s
    - 12,468 sps
  - redis-protocol/finagle 0.3-SNAPSHOT
    - finagle, netty 3, custom codec
    - 3.96 MB/s
    - 15,281 sps
  - redis-protocol/netty4 0.3-SNAPSHOT
    - netty 4, custom codec
    - 5.08 MB/s
    - 19,601 sps
  - redis-benchmark -n 1000000 -c 1 -r 1000000 set test test
    - C client included with distribution
    - 5.53 MB/s
    - 22,055 sps
  - JRedis a.0-SNAPSHOT
    - blocking i/o
    - 6.08 MB/s
    - 23,738 sps
  - Jedis 2.2.0-SNAPSHOT
    - blocking i/o
    - 6.11 MB/s
    - 24,001 sps
  - redis-protocol/finagle-service 0.3-SNAPSHOT
    - finagle, blocking i/o
    - 6.40 MB/s
    - 24,379 sps
  - redis-protocol/client 0.3-SNAPSHOT
    - blocking i/o
    - 6.72 MB/s
    - 25,795 sps


Copyright 2012 Sam Pullara

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

```