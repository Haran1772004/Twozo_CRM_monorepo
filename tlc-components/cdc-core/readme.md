# CDC Core - Change Data Capture Framework

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Core Components](#core-components)
    - [Transaction Management](#transaction-management)
    - [Scheduler System](#scheduler-system)
    - [Event Publishing](#event-publishing)
    - [Event Model](#event-model)
    - [Data Enrichment](#data-enrichment)
    - [Event Consumption](#event-consumption)
    - [Resource Registration](#resource-registration)
- [Data Flow](#data-flow)
- [Getting Started](#getting-started)
- [Troubleshooting](#troubleshooting)

## Overview

CDC Core is an enterprise-grade Change Data Capture (CDC) framework designed for capturing, processing, and distributing database change events in distributed systems. Built on a microservices architecture, it provides a robust, scalable solution for real-time data synchronization, event-driven architectures, and data pipeline orchestration.

### Key Features

- **Real-time Change Capture**: Captures database changes with minimal latency
- **Scalable Architecture**: Horizontal scaling support for high-throughput scenarios
- **Flexible Event Publishing**: Multiple publisher implementations (Kafka, custom adapters)
- **Data Enrichment Pipeline**: Extensible enrichment framework for augmenting event data
- **Transactional Guarantees**: Ensures at-least-once delivery semantics
- **Distributed Task Scheduling**: Fault-tolerant task execution and scheduling
- **Event Aggregation**: Supports both individual and aggregated event processing
- **Resource Management**: Dynamic resource registration and lifecycle management

### Use Cases

- Database replication and synchronization
- Building event-sourced systems
- Real-time analytics and reporting
- Cache invalidation strategies
- Audit logging and compliance tracking
- Microservices data consistency patterns

## Architecture

The CDC Core framework follows a layered architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                     Client Applications                      │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    CDC Core Framework                        │
│  ┌───────────┐  ┌──────────┐  ┌──────────┐  ┌───────────┐ │
│  │Transaction│  │Scheduler │  │Publisher │  │ Consumer  │ │
│  │  Manager  │  │  Engine  │  │  Layer   │  │   Layer   │ │
│  └───────────┘  └──────────┘  └──────────┘  └───────────┘ │
│  ┌───────────────────────────────────────────────────────┐ │
│  │              Event & Enrichment Layer                 │ │
│  └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│           External Systems (Kafka, Databases, etc.)          │
└─────────────────────────────────────────────────────────────┘
```

### Package Structure

```
com.tlc.cdc.core
├── transaction     # Transaction coordination and scheduling
├── scheduler       # Distributed task scheduling
├── publisher       # Event publication mechanisms
├── event           # Event model and data structures
├── enrichment      # Data enrichment pipeline
├── consumer        # Event consumption and processing
└── registrar       # Resource and service registration
```

## Core Components

### Transaction Management

**Package**: `com.tlc.cdc.core.transaction`

The transaction layer provides ACID guarantees for CDC operations and manages the coordination of change capture activities.

#### Key Components

**`TransactionScheduler`**
- Abstract base scheduler for transaction management
- Defines the contract for scheduling transactional CDC operations
- Handles transaction lifecycle: begin, commit, rollback
- Thread-safe transaction coordination

**`TransactionScheduler$Instance`**
- Singleton implementation ensuring single transaction scheduler instance
- Lazy initialization with double-checked locking
- Manages global transaction state

**`CdcTransactionScheduler`**
- CDC-specific implementation of transaction scheduling
- Integrates with the underlying database transaction management
- Supports distributed transaction coordination
- Provides transaction isolation level configuration

**`CdcTransactionScheduler$Instance`**
- Singleton access point for CDC transaction scheduler
- Ensures consistent transaction handling across the application

#### Responsibilities

- Transaction boundary management
- Change tracking within transaction scope
- Transaction-level event batching
- Rollback and recovery handling
- Transaction timeout management

---

### Scheduler System

**Package**: `com.tlc.cdc.core.scheduler`

The scheduler system provides distributed, fault-tolerant task execution capabilities for CDC operations.

### Event Publishing

**Package**: `com.tlc.cdc.core.publisher`

The publisher layer handles the reliable delivery of CDC events to downstream systems.

**`CdcAggregateEventPublisher`**
- Publishes aggregated events for batch processing scenarios
- Reduces downstream load by combining related events
- Configurable aggregation windows and strategies
- Supports windowing functions (tumbling, sliding, session)

---

### Event Model

**Package**: `com.tlc.cdc.core.event`

The event package defines the data structures and factories for CDC events.

#### Event Schema

```json
{
  "timestamp": "ISO-8601",
  "op": "INSERT|UPDATE|DELETE",
  "ts_ms": 1768806546138,
  "transaction": null,
  "source": {
    "database": "string",
    "schema": "string",
    "table": "string",
    "version": "2.5.0.Final",
    "ts_ms": 1768806546101,
    "snapshot": "false",
    "db": "string",
    "sequence": "[\"68327288\",\"68340336\"]",
    "txId": 7391,
    "lsn": 68340336,
    "xmin": null
  },
  "before": { /* previous state for UPDATE/DELETE */ },
  "after": { /* current state for INSERT/UPDATE */ },
  "metadata": {
    "transactionId": "string",
    "position": "long",
    "enrichments": {}
  }
}
```

---

### Data Enrichment

**Package**: `com.tlc.cdc.core.enrichment`

The enrichment layer allows augmentation of CDC events with additional context and data.

---

### Event Consumption

**Package**: `com.tlc.cdc.core.consumer`

The consumer layer provides flexible mechanisms for processing CDC events.

---

### Resource Registration

**Package**: `com.tlc.cdc.core.registrar`

The registrar manages dynamic resource registration and service discovery.

## Data Flow

### Standard Event Flow

```
1. Source Database Change
         ↓
2. Transaction Scheduler (captures change)
         ↓
3. Event Creation (CdcEventDataFactory)
         ↓
4. Enrichment Pipeline (CdcEnrichment)
         ↓
5. Event Publishing (CdcEventPublisher)
         ↓
6. Message Broker (Kafka)
         ↓
7. Event Consumer (CdcEventConsumer)
         ↓
8. Business Logic Processing
         ↓
9. Offset Commit / Acknowledgment
```

### Aggregated Event Flow

```
1. Multiple Source Changes
         ↓
2. Event Buffering
         ↓
3. Aggregation Window Processing
         ↓
4. Aggregated Event Publishing (CdcAggregateEventPublisher)
         ↓
5. Kafka (aggregated topic)
         ↓
6. Aggregated Consumer (AggregatedCdcEventConsumer)
         ↓
7. Batch Processing
         ↓
8. Acknowledgment
```

## Getting Started

#### Maven Dependency

```xml
<dependency>
    <groupId>com.tlc.cdc</groupId>
    <artifactId>cdc-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Troubleshooting

### Common Issues

**Issue: High Consumer Lag**
```bash
# Check Kafka lag
kafka-consumer-groups.sh --bootstrap-server kafka:9092 \
  --group cdc-consumer-group --describe

# Solutions:
# 1. Increase consumer concurrency
# 2. Scale horizontally (add more consumer instances)
# 3. Optimize event processing logic
# 4. Increase partition count
```

**Issue: Out of Memory**
```bash
# Analyze heap dump
jmap -dump:format=b,file=heap.bin <pid>

# Solutions:
# 1. Increase heap size (-Xmx)
# 2. Reduce batch sizes
# 3. Implement backpressure
# 4. Enable G1GC
```