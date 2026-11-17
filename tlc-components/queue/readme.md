# Messaging Queue System

Our messaging queue system is designed to facilitate publishing messages and consuming them across various modules using Kafka as the underlying provider.

## Overview

In our module, we have implemented a queue service that offers three core functionalities:

### 1. **Consumer Creation**

We provide two types of consumers:

#### 1.1 **Normal Consumer**

This consumer supports the following functionalities:
- **Wakeup**: Used for graceful shutdown. If a consumer thread is blocked while processing messages, the `wakeup()` method can be used to safely interrupt the thread.
- **Records Polling**: Polls records from a partition based on the configured poll time.
- **Subscribe**: Subscribes to messages from specific topics.
- **Unsubscribe**: Unsubscribes from topics, often used during consumer shutdown.
- **Pause/Resume**: Pauses the consumer from polling messages temporarily and resumes when ready. Note: The consumer will not poll any messages until it is resumed.

#### 1.2 **Background Consumer**

The background consumer's core functionalities are defined internally. It operates in the background using configurations similar to those of a normal consumer. It automatically creates a consumer and manages the process using predefined Consumer services.

#### 1.3 **Consumer Configuration**

To create a consumer, you need to configure several key properties:
- **Subscription Topics**: The topics the consumer will listen to.
- **Poll Timing and Max Records**: Controls how often and how many records the consumer polls.
- **Auto-Commit Configuration**: Determines if the consumer should automatically commit offsets.
- **Back Pressure Control (Max Queue Size)**: Controls how many records can be processed at a time to avoid overloading the system. For example, if the max queue size is 5 and 4 records are being processed, only one more record can be processed before the consumer is paused. Once offsets are committed and slots are freed, the consumer will resume polling.

In addition, we have a **StreamListener** that listens for and processes consumed stream messages.

### 2. **Producer Creation**

Producers can be created with basic configurations, including the subscription topic to which the producer will publish messages.

### 3. **Kafka Queue Management**

We use Kafka to manage the messaging queue system. Here’s how our Kafka integration works:
- **Offset Management**: Messages are processed only if their offset is not already being processed. Once processing is complete, offsets are committed.
- **Context Loading**: While processing messages, we load the associated context values that were published with the message.
- **Auto-Commit Disabled**: For consumers with auto-commit disabled, it is crucial to call `StreamMessage.clean()` after processing to commit the offset and release the slot.

### 4. **Error Handling**

In case of an exception while processing records, the consumer is restarted gracefully to ensure no messages are lost.

## Configuration Parameters

To set up the messaging system in your application, the following configurations need to be provided:

- **consumerBufferBytes**: Configures the buffer size for the consumer.
- **heartbeatInterval**: The interval between heartbeats to the Kafka broker.
- **sessionTimeout**: The maximum amount of time that the broker will wait for a heartbeat before considering the consumer dead.
- **maxPollInterval**: The maximum delay between poll operations before the consumer is considered unresponsive.
- **provider**: Specifies the Kafka provider (e.g., `SelfManagedKafka`, `ManagedKafka`).

### Kafka Provider Configuration

#### Kafka Server Based
```yaml
kafka:
  selfManaged:
    server: {SELF_MANAGED_KAFKA_SERVER_ENDPOINT}
```

#### Kafka Serverless
```yaml
kafka:
  managed:
    server: ${MANAGED_SERVER_ENDPOINT} #Eg: local-test.servicebus.windows.net:9093
    security.protocol: SASL_SSL
    provider: azure
    aws:
      sasl:
        mechanism: AWS_MSK_IAM
        jaas.config: software.amazon.msk.auth.iam.IAMLoginModule required;
        client.callback.handler.class: software.amazon.msk.auth.iam.IAMClientCallbackHandler
    azure:
      sasl:
        mechanism: OAUTHBEARER
        jaas.config: org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;
        login.callback.handler.class: com.tlc.queue.internal.kafka.security.handler.AzureAuthCallbackHandler
        auth.callback:
          scope: <resource_url>/.default #Eg: https://local-test.servicebus.windows.net/.default
          principal: azure-cli

```

### Topics

You will also need to configure the topics the consumers and producers will use.

## Key Considerations

While working with consumers, pay special attention to the following configurations:
- **Heartbeat Interval**
- **Session Timeout**
- **Max Poll Interval**

These configurations are essential to ensure proper coordination between the consumer and Kafka broker.
In Kafka, **heartbeat interval**, **session timeout**, and **max poll interval** are crucial for maintaining a stable connection between the consumer and the Kafka broker. These settings ensure that consumers stay responsive and handle messages efficiently.

## 1. Heartbeat Interval (`heartbeat.interval.ms`)

- **Purpose**: The heartbeat interval is the frequency at which the consumer sends a signal (heartbeat) to the Kafka broker to indicate it’s alive.
- **How it Works**:
    - If the broker doesn’t receive a heartbeat within the `session.timeout.ms` period, it assumes the consumer is dead and triggers a rebalance.
    - The interval should be small enough to avoid missed heartbeats but not too frequent to cause unnecessary load on the broker.

- **Recommended Setting**:
    - Set to about **3 seconds** (3000 ms), which is typically one-third of the `session.timeout.ms` value.

  ```yaml
  heartbeat.interval.ms=3000
  ```

## 2. Session Timeout (`session.timeout.ms`)

- **Purpose**: Defines the maximum time a consumer can go without sending a heartbeat before the broker considers it dead and triggers a rebalance.
- **How it Works**:
    - Too short a timeout could cause unnecessary rebalances due to transient network issues.
    - Too long a timeout could delay the detection of a truly dead consumer.

- **Recommended Setting**:
    - Typically **30 seconds** (30000 ms). The session timeout should always be greater than `heartbeat.interval.ms`, usually by a factor of 10.

  ```yaml
  session.timeout.ms=30000
  ```

## 3. Max Poll Interval (`max.poll.interval.ms`)

- **Purpose**: Specifies the maximum time allowed between calls to `poll()`. This ensures the consumer is actively polling Kafka, even if it’s busy processing messages.
- **How it Works**:
    - If the consumer exceeds the `max.poll.interval.ms`, the broker will assume the consumer is unresponsive and trigger a rebalance.

- **Recommended Setting**:
    - Generally, **5 minutes** (300000 ms) is a good balance, allowing enough time for the consumer to process large or complex messages before polling again.

  ```yaml
  max.poll.interval.ms=300000
  ```

## Best Practices for Defining Values

- **Balance Responsiveness and Stability**: Avoid setting these values too low, as that could lead to unnecessary rebalances. Setting them too high may cause delays in detecting failed consumers.
- **Heartbeat Interval**: Should be around 1/3rd of the session timeout.
- **Session Timeout**: Should allow time for network delays but still detect dead consumers quickly.
- **Max Poll Interval**: Should accommodate your message processing workload. For long-running processes, set it higher.

## Example Kafka Consumer Configuration:

```yaml
heartbeat.interval.ms=3000
session.timeout.ms=30000
max.poll.interval.ms=300000
```

### Summary:

- **Heartbeat Interval**: Set it frequent enough to keep the consumer alive (e.g., 3 seconds).
- **Session Timeout**: Define how long a consumer can go without heartbeats before it’s considered dead (30 seconds).
- **Max Poll Interval**: Time allowed between polls to avoid rebalancing for slow message processing (5 minutes).

By adjusting these settings based on your system's needs, you ensure Kafka consumers remain reliable and efficient.


------------------------------------------------------------------------------------------------------------------------------


# KAFKA CONNECT

## Overview

This project provides an abstraction layer and orchestration mechanism
over **Kafka Connect** to manage Source and Sink connectors
programmatically.

The design follows a structured service-based architecture to:

-   Create connectors
-   Update connectors
-   Delete connectors
-   Check connector status
-   Stop connectors
-   Separate Source and Sink logic cleanly
-   Centralize configuration handling



## Architecture Overview

The system is organized into the following layers:

    com.tlc
     └── commons
          └── service (Base Service abstraction)

    cdc.core.connect
     ├── KafkaConnectService (Entry Interface)
     ├── KafkaConnectOrchestrator (Coordinator)
     ├── AbstractConnectorService (Base Implementation)
     ├── ConnectorService (Common Connector Contract)
     ├── ConnectorConfig (Base Config Model)
     ├── Type (SOURCE / SINK enum)
     ├── source
     │    ├── SourceConnectorService
     │    └── SourceConnectorConfig
     └── sink
          ├── SinkConnectorService
          └── SinkConnectorConfig



## Core Components

### 1. KafkaConnectService (Interface)

Entry-level abstraction for obtaining connector services.

``` java
SourceConnectorService sourceConnector();
SinkConnectorService sinkConnector();
```


### 2. KafkaConnectOrchestrator

Acts as a central coordinator.

Responsibilities:

-   Initialize connectors
-   Provide access to source and sink services
-   Manage lifecycle (start / stop)

Singleton-style instance handling.



### 3. ConnectorService (Interface)

Defines the common contract for all connectors:

``` java
void create(C config);
void update(C config);
void delete(C config);
void status(C config);
void stop();
```


### 4. AbstractConnectorService

Base implementation containing shared logic:

-   REST call handling
-   HTTP methods integration
-   Logging
-   Common request/response handling

Common operations:

``` java
JsonObject connect(String, HttpMethod, JsonObject)
JsonObject delete(String, String)
JsonObject restart(String, String)
JsonObject status(String, String)
```


### 5. SourceConnectorService

Concrete implementation for **Source Connectors**.

Handles:

-   Create Source connector
-   Update configuration
-   Delete connector
-   Check status
-   Stop connector

Uses: - `SourceConnectorConfig`


### 6. SinkConnectorService

Concrete implementation for **Sink Connectors**.

Handles:

-   Create Sink connector
-   Update configuration
-   Delete connector
-   Check status
-   Stop connector

Uses: - `SinkConnectorConfig`


### 7. ConnectorConfig

Common configuration model:

Fields:

-   `String name`
-   `String url`
-   `JsonObject config`
-   `Type type`

Provides:

-   Getter/Setter methods
-   `toRequestJson()` for Kafka Connect REST API payload


### 8. Type Enum

``` java
SOURCE
SINK
```

Used to differentiate connector behavior.

## How It Works

1.  Application initializes `KafkaConnectOrchestrator`
2.  Orchestrator provides access to:
  -   `SourceConnectorService`
  -   `SinkConnectorService`
3.  Config objects are created
4.  Services call Kafka Connect REST API
5.  Response is returned as JsonObject


## Example Usage

``` java
SourceConnectorService sourceService = 
    KafkaConnectOrchestrator.getInstance().sourceConnector();

SourceConnectorConfig config = new SourceConnectorConfig();
config.setName("crm-source");
config.setUrl("http://localhost:8083");
config.setConfig(jsonPayload);

sourceService.create(config);
```

## CDC Pipeline Configuration - Source & Sink Connectors

## Source Connector (Debezium PostgreSQL - CDC)

This connector captures real-time database changes from PostgreSQL using
Change Data Capture (CDC) and publishes them to Kafka topics.

### Basic Settings

-   name: source-connector
-   connector.class: io.debezium.connector.postgresql.PostgresConnector
-   plugin.name: pgoutput
-   topic.prefix: crm

The topic prefix determines how Kafka topics are named. Example:
crm.public.contact

### Database Connection

-   database.hostname: postgres
-   database.port: 5432
-   database.user: postgres
-   database.password: root123
-   database.dbname: TLC

These properties define the PostgreSQL connection details.

### Logical Replication (CDC Core)

-   slot.name: crm_slot
-   publication.name: crm_pub
-   publication.autocreate.mode: filtered
-   schema.include.list: public
-   table.include.list: contact,company

Explanation:

-   slot.name → Maintains WAL position for CDC streaming.
-   publication.name → Defines which tables are part of logical
    replication.
-   publication.autocreate.mode=filtered → Auto-creates publication only
    for included tables.
-   schema.include.list → Captures only selected schemas.
-   table.include.list → Captures only specific tables.

This ensures controlled and filtered CDC capture.

### Snapshot Mode

-   snapshot.mode: initial

Takes an initial full snapshot of existing data before switching to
real-time streaming.

### Heartbeat

-   heartbeat.interval.ms: 20000

Sends heartbeat every 20 seconds to prevent replication slot lag issues.

### Kafka Serialization

-   key.converter: org.apache.kafka.connect.json.JsonConverter
-   value.converter: org.apache.kafka.connect.json.JsonConverter
-   key.converter.schemas.enable: false
-   value.converter.schemas.enable: false

Produces schema-less JSON messages in Kafka.

### Schema History

-   schema.history.internal.kafka.bootstrap.servers: kafka:9092

Used internally by Debezium to track schema changes.

------------------------------------------------------------------------

## Sink Connector (ClickHouse Sink)

This connector consumes Kafka topics and inserts records into ClickHouse
tables.

### Basic Settings

-   name: sink-connector
-   connector.class:
    com.clickhouse.kafka.connect.ClickHouseSinkConnector
-   topics: DEAL,COMPANY,PRODUCT
-   tasks.max: 12

tasks.max controls parallelism. Higher value improves throughput if
topic partitions support it.

### ClickHouse Connection

-   hostname: clickhouse
-   port: 8123
-   database: TLC
-   username: default
-   password: root123

Defines the ClickHouse database connection details.

### Table Mapping

-   clickhouse.table.name.format: \${topic}
-   auto.create.tables: false

Each Kafka topic writes to a ClickHouse table with the same name. Tables
must already exist.

### Performance Tuning

-   batch.size: 1000

Controls number of records per insert batch. Larger batch improves
performance but increases memory usage.

### Kafka Converters

-   key.converter: org.apache.kafka.connect.storage.StringConverter
-   value.converter: org.apache.kafka.connect.json.JsonConverter
-   value.converter.schemas.enable: false

### Error Handling & Dead Letter Queue (DLQ)

-   errors.tolerance: all
-   errors.log.enable: true
-   errors.log.include.messages: true
-   errors.log.include.messages: true
-   errors.deadletterqueue.topic.name: dlq-facts
-   errors.deadletterqueue.topic.replication.factor: 1
-   errors.deadletterqueue.context.headers.enable: true

If a record fails:

1.  Connector does not stop
2.  Error is logged
3.  Failed record is sent to DLQ topic (dlq-facts)

This ensures pipeline reliability.

------------------------------------------------------------------------
### Conclusion

This configuration establishes a reliable CDC pipeline:

PostgreSQL → Debezium → Kafka → ClickHouse

It ensures controlled capture, scalable ingestion, and resilient error
handling for production-ready systems.
