package com.tlc.web.client.internal.cluster.strategy;

import com.tlc.web.client.internal.cluster.ClusterRecoveryCoordinator;
import com.tlc.web.client.internal.cluster.VertxLifecycle;
import com.tlc.web.client.registrar.WebClientResourceRegistrar;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.core.spi.cluster.NodeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Membership node listener: self-left always reboots; peer join reboots only for requester nodes.
 *
 * <p>Node listener callbacks must not acquire the Vert.x lifecycle lock; they delegate to the
 * coordinator via lock-free reboot requests only.</p>
 *
 * @author sundar
 * @version 1.0
 */
public final class PeerJoinRebootStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeerJoinRebootStrategy.class);

    private final ClusterRecoveryCoordinator coordinator;

    /**
     * Creates a peer-join reboot strategy bound to the recovery coordinator.
     *
     * @param coordinator recovery coordinator
     */
    public PeerJoinRebootStrategy(final ClusterRecoveryCoordinator coordinator) {
        this.coordinator = Objects.requireNonNull(coordinator);
    }

    /**
     * Installs a cluster membership listener for node join and leave events.
     *
     * @param manager active cluster manager
     */
    public void install(final ClusterManager manager) {
        if (Objects.isNull(manager)) {
            LOGGER.warn("Cluster manager is null; membership reboot listener not installed");
            return;
        }

        final String selfNodeId = manager.getNodeId();
        final VertxLifecycle lifecycle = coordinator.getLifecycle();
        coordinator.setAcceptingMembershipEvents(true);
        manager.nodeListener(new NodeListener() {
            @Override
            public void nodeAdded(final String nodeID) {
                onPeerNodeAdded(nodeID, selfNodeId, lifecycle);
            }

            @Override
            public void nodeLeft(final String nodeID) {
                if (Objects.equals(selfNodeId, nodeID)) {
                    onSelfNodeLeft(nodeID, lifecycle);
                } else {
                    onPeerNodeLeft(nodeID, lifecycle);
                }
            }
        });
        LOGGER.info("Vert.x cluster membership listener installed for nodeId={}", selfNodeId);
    }

    /**
     * Schedules a deferred reboot when a peer joins on a requester node.
     *
     * @param nodeID      id of the node that joined
     * @param selfNodeId  id of this node
     * @param lifecycle   Vert.x lifecycle port
     */
    private void onPeerNodeAdded(final String nodeID, final String selfNodeId, final VertxLifecycle lifecycle) {
        LOGGER.debug("Vert.x cluster node added: {}", nodeID);
        if (!coordinator.isAcceptingMembershipEvents()
                || lifecycle.isStopped()
                || Objects.equals(selfNodeId, nodeID)) {
            return;
        }
        if (WebClientResourceRegistrar.getInstance().hostsEventConsumers()) {
            LOGGER.info("Peer node {} joined; skipping reboot (this node hosts event consumers)", nodeID);
            return;
        }
        LOGGER.warn("Peer node {} joined; scheduling in-JVM reboot to refresh cluster routing", nodeID);
        coordinator.requestRebootAfterPeerJoin();
    }

    /**
     * Requests reboot when this node leaves the cluster unexpectedly.
     *
     * @param nodeID    id of the node that left (this node)
     * @param lifecycle Vert.x lifecycle port
     */
    private void onSelfNodeLeft(final String nodeID, final VertxLifecycle lifecycle) {
        if (!coordinator.isAcceptingMembershipEvents()) {
            return;
        }
        if (lifecycle.isStopped()) {
            return;
        }
        LOGGER.error("This Vert.x node ({}) left the cluster; requesting in-JVM reboot", nodeID);
        coordinator.requestReboot();
    }

    /**
     * Requests reboot when a peer leaves during cluster instability.
     *
     * @param nodeID    id of the peer that left
     * @param lifecycle Vert.x lifecycle port
     */
    private void onPeerNodeLeft(final String nodeID, final VertxLifecycle lifecycle) {
        if (!coordinator.isAcceptingMembershipEvents()) {
            return;
        }
        LOGGER.info("Vert.x cluster node left: {}", nodeID);
        if (!lifecycle.isStopped() && coordinator.isClusterMembershipUnstable()) {
            LOGGER.warn("Peer node {} left during cluster instability; requesting in-JVM reboot", nodeID);
            coordinator.requestReboot();
        }
    }
}
