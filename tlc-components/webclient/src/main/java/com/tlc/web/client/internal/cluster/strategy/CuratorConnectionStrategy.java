package com.tlc.web.client.internal.cluster.strategy;

import com.tlc.web.client.internal.cluster.ClusterRecoveryCoordinator;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Arms reboot when the ZooKeeper Curator session is lost or reconnects after an outage.
 *
 * <p>Curator callbacks must not acquire the Vert.x lifecycle lock; they delegate to the coordinator
 * via lock-free atomic state updates only.</p>
 *
 * @author sundar
 * @version 1.0
 */
public final class CuratorConnectionStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(CuratorConnectionStrategy.class);

    private final ClusterRecoveryCoordinator coordinator;
    private final AtomicReference<CuratorFramework> curatorFrameworkRef = new AtomicReference<>();
    private final AtomicReference<ConnectionStateListener> curatorConnectionListenerRef = new AtomicReference<>();

    /**
     * Creates a Curator connection strategy bound to the recovery coordinator.
     *
     * @param coordinator recovery coordinator
     */
    public CuratorConnectionStrategy(final ClusterRecoveryCoordinator coordinator) {
        this.coordinator = Objects.requireNonNull(coordinator);
    }

    /**
     * Installs a Curator connection-state listener on ZooKeeper-backed cluster managers.
     *
     * @param manager active cluster manager
     */
    public void install(final ClusterManager manager) {
        if (!(manager instanceof ZookeeperClusterManager zkManager)) {
            return;
        }

        final CuratorFramework curator = zkManager.getCuratorFramework();
        if (Objects.isNull(curator)) {
            LOGGER.warn("CuratorFramework unavailable; ZK connection-state listener not installed");
            return;
        }

        remove();

        final ConnectionStateListener listener = (client, newState) -> handleConnectionState(newState);
        curator.getConnectionStateListenable().addListener(listener);
        curatorFrameworkRef.set(curator);
        curatorConnectionListenerRef.set(listener);
        LOGGER.info("ZooKeeper Curator connection-state listener installed");
    }

    /**
     * Removes the Curator connection-state listener if previously installed.
     */
    public void remove() {
        final CuratorFramework curator = curatorFrameworkRef.getAndSet(null);
        final ConnectionStateListener listener = curatorConnectionListenerRef.getAndSet(null);

        if (Objects.nonNull(curator) && Objects.nonNull(listener)) {
            try {
                curator.getConnectionStateListenable().removeListener(listener);
            } catch (Exception exception) {
                LOGGER.warn("Failed to remove Curator connection-state listener: {}", exception.getMessage());
            }
        }
    }

    /**
     * Handles Curator session state transitions and arms reboot when appropriate.
     *
     * @param newState new Curator connection state
     */
    private void handleConnectionState(final ConnectionState newState) {
        if (!coordinator.isAcceptingMembershipEvents() || coordinator.getLifecycle().isStopped()) {
            return;
        }

        switch (newState) {
            case LOST -> {
                coordinator.markUnhealthy();
                LOGGER.error(
                        "ZooKeeper Curator connection LOST; cluster marked unhealthy (reboot deferred until RECONNECTED)");
            }
            case SUSPENDED -> {
                coordinator.markUnhealthy();
                LOGGER.warn("ZooKeeper Curator connection SUSPENDED; cluster marked unhealthy");
            }
            case RECONNECTED -> {
                if (coordinator.isClusterUnhealthy()) {
                    LOGGER.warn("ZooKeeper Curator RECONNECTED after outage; requesting in-JVM Vert.x cluster reboot");
                    coordinator.requestReboot();
                }
            }
            default -> LOGGER.debug("ZooKeeper Curator connection state: {}", newState);
        }
    }
}
