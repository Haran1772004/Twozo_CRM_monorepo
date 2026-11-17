
# Configuring Cache Service in `application.yml`

To start with the cache service, ensure that the `cache` service is enabled in your `application.yml`.  
You must add `cache` under the `services.enabled` section.

## Example Configuration

```yaml
services:
  enabled:
    - cache

cache:
  type: SINGLE
  scheme: redis
  server: ${REDIS_SERVER}
  password: ${REDIS_PASSWORD}
  test-enabled: false
```

## Configuration Details

- **type**
    - **Mandatory**
    - Represents the server type. Possible values:
        - `SINGLE` → Single Redis instance
        - `CLUSTER` → Redis cluster
        - `SENTINEL` → Redis Sentinel setup

- **scheme**
    - **Optional (defaults to `redis`)**
    - Represents the Redis connection scheme.
    - By default, it takes `redis`.

- **server**
    - **Mandatory**
    - Specifies the hostname and port of the Redis server.
    - Example: `localhost:6379`

- **password**
    - **Optional**
    - The authentication password (or access key) required to connect to the Redis server.
    - Example: `${REDIS_PASSWORD}`

- **test-enabled**
    - **Optional (default: `false`)**
    - If enabled, once the Redisson client is created, it will:
        1. Create a test cache.
        2. Put a dummy value.
        3. Retrieve the value to verify connectivity.

## Notes for Azure Cache for Redis

- For Azure Redis, you **must** specify the scheme as `rediss` to ensure SSL-secured communication.
- Ensure you configure the **Access Key** or **Azure AD token authentication** properly.
- Use port `6380` (default SSL port).  

# Redis Atomic Service (Distributed Counter Utility)

## Overview

`RedisAtomicService` defines an abstraction for performing **atomic numeric operations** backed by **Redis**.  
It allows multiple JVM instances to safely update shared counters in a **thread-safe** and **distributed** manner.

## Architecture

The `DistributedAtomicCounter` acts as a **wrapper** around Redis primitives (e.g., Redisson’s `RAtomicLong`) to expose a
simple, pluggable interface.

### Create an atomic counter instance

Each Redis key uniquely identifies one distributed counter.

```java
DistributedAtomicCounter counter = cacheService.getOrInitAtomicCounter("app:module:counterName");
```

### Tips

- Use **namespaced keys** (`app:module:counterName`) to avoid collisions.
- Always use **`compareAndSet`** for safe conditional updates.
- Combine with **`RLock`** if you need stronger transaction-like guarantees.
