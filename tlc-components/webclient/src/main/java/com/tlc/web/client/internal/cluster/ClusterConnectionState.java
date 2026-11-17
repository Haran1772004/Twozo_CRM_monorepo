package com.tlc.web.client.internal.cluster;

/**
 * Explicit connection state for Vert.x cluster recovery.
 *
 * @author sundar
 * @version 1.0
 */
public enum ClusterConnectionState {

    /** Normal operation before any Curator or membership instability. */
    HEALTHY,

    /** Curator session lost or suspended; traffic gated until RECONNECTED triggers reboot. */
    UNHEALTHY,

    /** In-JVM Vert.x teardown or rejoin is in progress. */
    REBOOTING,

    /** Rejoin succeeded but requester nodes are still waiting for peer visibility. */
    AWAITING_PEERS,

    /** Membership and routing checks passed; clustered traffic is allowed. */
    READY
}
