# Java SDK Quickstart

This repository contains the skeleton code you need to run the Kafka Streams `WordCount` example
as well as migrate that code to Responsive. This quickstart:

1. Spins up a Kafka Broker and a MongoDB instance
1. Runs a `KafkaStreams` application
1. Migrates to `ResponsiveKafkaStreams` and runs it
1. Queries MongoDB to demonstrate disaggregated state

## Requirements

In order to run this quickstart, you must have access to the following systems:

- Docker and Docker Compose (or a compatible engine)
- Gradle (it will install JDK 21 if you do not have it installed)

## Prepare Dependencies

The first step is to deploy a Kafka broker and a MongoDB instance. You can use the
following `docker-compose.yml` file (which is also included in `<root>/src` of this repository):

```yaml docker-compose.yml
---
services:
  broker:
    image: confluentinc/cp-kafka:7.7.1
    container_name: kafka-broker
    ports:
      - "9092:9092"
    environment:
      CLUSTER_ID: 'jHS82zyorYvKMntfzD4XRQ'
      KAFKA_PROCESS_ROLES: 'controller,broker'
      KAFKA_NODE_ID: 1
      KAFKA_CONTROLLER_LISTENER_NAMES: 'CONTROLLER'
      KAFKA_INTER_BROKER_LISTENER_NAME: 'INTER_BROKER'
      KAFKA_LISTENERS: 'INTER_BROKER://:29092,PUBLIC://:9092,CONTROLLER://:9093'
      KAFKA_CONTROLLER_QUORUM_VOTERS: '1@localhost:9093'
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: 'INTER_BROKER:PLAINTEXT,PUBLIC:PLAINTEXT,CONTROLLER:PLAINTEXT'
      KAFKA_ADVERTISED_LISTENERS: 'INTER_BROKER://localhost:29092,PUBLIC://localhost:9092'
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_LOG_RETENTION_CHECK_INTERVAL_MS: 1000

  mongo:
    image: mongo:5.0
    container_name: mongo
    ports:
      - "27017:27017"
```

After these spin up, we will need to create topics in the broker for
the Kafka Streams application to use. You can do this by executing
into the docker container and running the following commands:

```shell
$ docker exec -it kafka-broker bash

$ kafka-topics --create \
    --bootstrap-server localhost:9092 \
    --replication-factor 1 \
    --partitions 1 \
    --topic words
    
$ kafka-topics --create \
    --bootstrap-server localhost:9092 \
    --replication-factor 1 \
    --partitions 1 \
    --config cleanup.policy=compact \
    --topic output
```

## Build & Run

Start by building the code.

```shell
./gradlew build
```

The application `dev.responsive.quickstart.WordCount` is identical
to the open source [word count](https://kafka.apache.org/39/documentation/streams/quickstart) app
and counts the number of occurrences of each word in the input
topic, split by whitespace. To run the application, use:

```shell
./gradlew run
```

### Produce & Consume

Let's produce some data in the `words` topic, and then verify that
the application is counting those words! First, produce using `kafka-console-producer`

```bash
$ kafka-console-producer --bootstrap-server localhost:9092 --topic words
> all streams flow through kafka streams
```

You can verify that the word count app is working as designed by consuming
from the output topic using `kafka-console-consumer`:

```bash
kafka-console-consumer \
     --bootstrap-server localhost:9092 \
     --topic output \
     --from-beginning \
     --property print.key=true \
     --property print.value=true \
     --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
     --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
```

Wait a while to make sure the buffering time limits have elapsed, and you should see the following
output:

```txt
all	1
flow	1
through	1
kafka	1
streams	2
```

# Use the Responsive SDK

Introducing the Responsive SDK for an app that only uses the DSL
is as simple as it gets:

1. Add the required imports:
```java
import dev.responsive.kafka.api.ResponsiveKafkaStreams;
import dev.responsive.kafka.api.config.ResponsiveConfig;
import dev.responsive.kafka.api.config.StorageBackend;
```
2. Change `new KafkaStreams` to `new ResponsiveKafkaStreams`:
```diff
- final KafkaStreams streams = new KafkaStreams(topology, props);
+ final KafkaStreams streams = new ResponsiveKafkaStreams(topology, props);
```
3. Add the configurations required to talk to MongoDB:
```java
props.put(ResponsiveConfig.STORAGE_BACKEND_TYPE_CONFIG, StorageBackend.MONGO_DB.name());
props.put(ResponsiveConfig.MONGO_ENDPOINT_CONFIG, "mongodb://localhost:27017");
props.put(ResponsiveConfig.RESPONSIVE_LICENSE_CONFIG, "<your-license>");
```

> Note: if you have not yet obtained a trial license for the ResponsiveSDK,
> make sure to get a free one [here](https://responsive.dev/sdk/get-started) (it takes 2 minutes and
> its free).

That's all you have to do! Underneath the hood, this replaces a bunch
of components of Kafka Streams from the state storage to the way
restoration and commits work so that you can leverage remote state. 

These lines of code are the only changes you need to make if you only use the DSL (this example fits
this category), if you manually
specify stores using the `Materialized` API or use the Processor API
read [the documentation](https://docs.responsive.dev/getting-started/migrate) to get a sense of how
to migrate individual stores. We promise it's just as easy.

## Build & Run

When you run the Responsive version of the app for the first time,
it will read the existing changelog topic and bootstrap the remote
store the same way a normal restore works with RocksDB. The difference
is that this is the last restore your app will ever need to do:

```shell
./gradlew build
./gradlew run
```

You can confirm that the bootstrap was successful by querying MongoDB
directly:

```shell
$ docker exec -it mongo mongosh

test> use counts_store;
switched to db counts_store
counts_store> db.kv_data.countDocuments();
5
```

Finally, you can produce more data using the console producer
and confirm that it works using the console producer with the
same steps outlined above.

# Next Steps

Congrats! You have successfully run through the quickstart for the
Responsive SDK. Here's what you can do next:

// TODO(agavra): add links to these steps below

1. Test this on one of your existing apps. Read the more detailed migration instructions for
   onboarding more complicated apps.
1. Read about our advanced functionality like async processing or row-level TTL.
1. Explore our other products like the Control Plane and RS3.