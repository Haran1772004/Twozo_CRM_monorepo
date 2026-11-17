package com.tlc.web.client.internal.cluster.strategy;

import com.tlc.web.client.internal.cluster.ClusterConnectionState;
import com.tlc.web.client.internal.cluster.ClusterRecoveryCoordinator;
import com.tlc.web.client.registrar.WebClientResourceRegistrar;
import io.vertx.core.Vertx;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Periodic and on-demand membership/routing readiness checks for ZooKeeper-backed Vert.x clusters.
 *
 * @author sundar
 * @version 1.0
 */
public final class MembershipHealthStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(MembershipHealthStrategy.class);

    /** Require peers visible across consecutive health ticks to ignore ephemeral ZK ghost nodes. */
    private static final int PEER_STABLE_TICKS_REQUIRED = 2;

    private final ClusterRecoveryCoordinator coordinator;
    private final AtomicInteger peerVisibleTicks = new AtomicInteger(0);

    /**
     * Creates a membership health strategy bound to the recovery coordinator.
     *
     * @param coordinator recovery coordinator
     */
    public MembershipHealthStrategy(final ClusterRecoveryCoordinator coordinator) {
        this.coordinator = Objects.requireNonNull(coordinator);
    }

    /**
     * Evaluates cluster membership and routing readiness; requests reboot when checks fail.
     */
    public void evaluate() {
        if (coordinator.getLifecycle().isStopped() || coordinator.isRebootRequested()) {
            return;
        }

        final Vertx vertx = coordinator.getLifecycle().getVertxOrNull();
        if (Objects.isNull(vertx) || !vertx.isClustered()) {
            handleUnclusteredVertx();
            return;
        }

        final ClusterManager manager = coordinator.getLifecycle().getClusterManager();
        if (manager instanceof ZookeeperClusterManager zkManager) {
            if (evaluateZookeeperHealth(zkManager)) {
                return;
            }
        }

        clearUnhealthyIfRecovered();
    }

    /**
     * Returns whether this node's id is present in the ZooKeeper membership list.
     *
     * @param zkManager ZooKeeper cluster manager
     * @return {@code true} when membership is ready for this node
     */
    public boolean isMembershipReady(final ZookeeperClusterManager zkManager) {
        if (!zkManager.isActive()) {
            return false;
        }
        final Vertx vertx = coordinator.getLifecycle().getVertxOrNull();
        if (Objects.isNull(vertx) || !vertx.isClustered()) {
            return false;
        }
        final String selfNodeId = zkManager.getNodeId();
        if (Objects.isNull(selfNodeId) || selfNodeId.isBlank()) {
            return false;
        }
        return zkManager.getNodes().contains(selfNodeId);
    }

    /**
     * Membership is ready for this node; routing is ready when requesters can reach peers that were
     * previously observed in the cluster (multi-service deployments after ZooKeeper recovery).
     *
     * <p>Requester nodes require peers to remain visible across consecutive {@link #evaluate()} ticks
     * so ephemeral post-outage ghost node IDs do not falsely mark routing ready.</p>
     *
     * @param zkManager ZooKeeper cluster manager
     * @return {@code true} when clustered routing is ready
     */
    public boolean isClusterRoutingReady(final ZookeeperClusterManager zkManager) {
        if (!isMembershipReady(zkManager)) {
            return false;
        }
        if (WebClientResourceRegistrar.getInstance().hostsEventConsumers()) {
            return true;
        }
        if (coordinator.getMaxObservedClusterSize() > 1) {
            return zkManager.getNodes().size() > 1
                    && peerVisibleTicks.get() >= PEER_STABLE_TICKS_REQUIRED;
        }
        return true;
    }

    /**
     * Updates peer-visibility stability once per health-check tick (call before {@link #isClusterRoutingReady}).
     *
     * @param zkManager ZooKeeper cluster manager
     */
    public void observePeerVisibility(final ZookeeperClusterManager zkManager) {
        if (zkManager.getNodes().size() > 1) {
            peerVisibleTicks.incrementAndGet();
        } else {
            peerVisibleTicks.set(0);
        }
    }

    /**
     * Resets peer-visibility stability counters before a reboot teardown.
     */
    public void resetPeerStability() {
        peerVisibleTicks.set(0);
    }

    /**
     * Returns whether clustered routing is ready for the active cluster manager.
     *
     * @return {@code true} when routing is ready
     */
    public boolean isClusterRoutingReady() {
        final ClusterManager manager = coordinator.getLifecycle().getClusterManager();
        if (manager instanceof ZookeeperClusterManager zkManager) {
            return isClusterRoutingReady(zkManager);
        }
        return true;
    }

    /**
     * Returns whether membership is unstable enough to warrant reboot on peer departure.
     *
     * @return {@code true} when membership is unstable
     */
    public boolean isClusterMembershipUnstable() {
        // AWAITING_PEERS is a stable wait state — peer left/ghost cleanup must not trigger reboot loops.
        if (coordinator.getConnectionState() == ClusterConnectionState.AWAITING_PEERS) {
            return false;
        }
        final ClusterManager manager = coordinator.getLifecycle().getClusterManager();
        return coordinator.isClusterUnhealthy()
                || (manager instanceof ZookeeperClusterManager zk && !isMembershipReady(zk));
    }

    /**
     * Logs membership details after a successful cluster rejoin.
     */
    public void logMembershipAfterRejoin() {
        final ClusterManager manager = coordinator.getLifecycle().getClusterManager();
        if (manager instanceof ZookeeperClusterManager zkManager) {
            coordinator.trackClusterSize(zkManager);
            final List<String> nodes = zkManager.getNodes();
            LOGGER.info("Vert.x cluster membership after rejoin: active={}, routingReady={}, nodeCount={}, nodes={}",
                    zkManager.isActive(), isClusterRoutingReady(zkManager), nodes.size(), nodes);
        }
    }

    /**
     * Marks the cluster unhealthy and requests reboot when Vert.x is not clustered.
     */
    private void handleUnclusteredVertx() {
        LOGGER.warn("Vert.x cluster health check failed: not clustered; requesting in-JVM reboot");
        coordinator.markUnhealthy();
        coordinator.requestReboot();
    }

    /**
     * Evaluates ZooKeeper membership and routing; requests reboot or enters AWAITING_PEERS when needed.
     *
     * @param zkManager ZooKeeper cluster manager
     * @return {@code true} when evaluation should stop (reboot requested or awaiting peers)
     */
    private boolean evaluateZookeeperHealth(final ZookeeperClusterManager zkManager) {
        coordinator.trackClusterSize(zkManager);

        final boolean active = zkManager.isActive();
        final boolean membershipReady = isMembershipReady(zkManager);
        if (!active || !membershipReady) {
            peerVisibleTicks.set(0);
            LOGGER.warn(
                    "Vert.x cluster health check failed: active={}, membershipReady={}, self={}, nodes={}; requesting in-JVM reboot",
                    active, membershipReady, zkManager.getNodeId(), zkManager.getNodes());
            coordinator.markUnhealthy();
            coordinator.requestReboot();
            return true;
        }

        observePeerVisibility(zkManager);

        if (!isClusterRoutingReady(zkManager)) {
            coordinator.markAwaitingPeers();
            if (peerVisibleTicks.get() <= 1) {
                final List<String> nodes = zkManager.getNodes();
                LOGGER.info(
                        "Vert.x cluster membership healthy but routing not ready yet; waiting for peers (self={}, nodeCount={}, maxObserved={}, peerTicks={})",
                        zkManager.getNodeId(), nodes.size(),
                        coordinator.getMaxObservedClusterSize(), peerVisibleTicks.get());
            }
            return true;
        }

        return false;
    }

    /**
     * Clears a prior non-UNHEALTHY instability flag after a successful health check.
     */
    private void clearUnhealthyIfRecovered() {
        LOGGER.debug("Vert.x cluster health check passed");
        // Do not clear curator-driven UNHEALTHY here — wait for RECONNECTED reboot (or READY/AWAITING_PEERS).
        if (coordinator.isClusterUnhealthy()
                && coordinator.getConnectionState() != ClusterConnectionState.UNHEALTHY) {
            LOGGER.info("Vert.x cluster health check passed after prior instability; clearing unhealthy flag");
            coordinator.clearUnhealthy();
        }
    }
}
