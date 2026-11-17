# Vert.x cluster recovery (ZooKeeper)

Recovery after a ZooKeeper outage is owned by `ClusterRecoveryCoordinator` and its strategies.
`VertxClient` only joins, closes, and holds Vert.x references.

**States:** `HEALTHY` → `UNHEALTHY` (Curator `SUSPENDED` / `LOST`) → `REBOOTING` (on `RECONNECTED`) → `AWAITING_PEERS` or `READY`.

## Happy path after join

```mermaid
flowchart LR
  A[joinCluster] --> B[Install listeners] --> C[Start scheduler] --> D[Idle health ticks]
```

## ZooKeeper outage → reboot → ready

```mermaid
flowchart TD
  A[Curator SUSPENDED or LOST] --> B[Mark UNHEALTHY<br/>gate traffic]
  B --> C[Wait — do not reboot]
  C --> D[Curator RECONNECTED]
  D --> E[requestReboot]
  E --> F[Discard consumers]
  F --> G[Close Vert.x]
  G --> H[joinCluster]
  H --> I[Rebind codecs + listeners]
  I --> J{Routing ready?}
  J -->|Yes| K[READY]
  J -->|No| L[AWAITING_PEERS]
```

## Scheduler health check only

```mermaid
flowchart TD
  A[Health tick] --> B{Membership OK?}
  B -->|No| C[requestReboot]
  B -->|Yes, peers missing| D[AWAITING_PEERS]
  B -->|Yes, routing ready| E[Clear unhealthy<br/>if not curator UNHEALTHY]
```

## Peer joins the cluster

```mermaid
flowchart TD
  A[nodeAdded] --> B{This node hosts consumers?}
  B -->|Yes — provider| C[Skip reboot]
  B -->|No — requester| D[Delay 3s then requestReboot]
```

## Sequence

1. Join installs Curator and membership listeners; the reboot scheduler ticks about every 5 seconds.
2. Outage: `SUSPENDED` / `LOST` marks the cluster unhealthy and **defers reboot**. Waiting for cluster unregister would hang while ZooKeeper is down.
3. `RECONNECTED` arms a single-flight in-JVM reboot.
4. Teardown uses `EventConsumerRegistrar.discardAllConsumers()` (no cluster wait).
5. After rejoin: `CodecRegistrar.rebindAll()` then `WebClientResourceRegistrar.reregisterAll()`.
6. A requester with `maxObservedClusterSize > 1` stays in `AWAITING_PEERS` until peers are visible for two health ticks. The health check must not reboot in that state.
7. A provider skips peer-join reboot. A requester schedules a 3 second delayed reboot to refresh routing.
8. Traffic gate: `WebClientServiceProvider.isClusterReadyForTraffic()`.
