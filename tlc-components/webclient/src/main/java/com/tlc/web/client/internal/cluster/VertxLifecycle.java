package com.tlc.web.client.internal.cluster;

import com.tlc.commons.util.Configurations;
import io.vertx.core.Vertx;
import io.vertx.core.spi.cluster.ClusterManager;

import java.util.function.Supplier;

/**
 * Narrow port for Vert.x lifecycle operations used by {@link ClusterRecoveryCoordinator}.
 * Implemented by {@link com.tlc.web.client.internal.VertxClient}.
 *
 * @author sundar
 * @version 1.0
 */
public interface VertxLifecycle {

    /**
     * Returns whether the Vert.x client has been stopped.
     *
     * @return {@code true} when stopped
     */
    boolean isStopped();

    /**
     * Returns the active Vert.x instance, or {@code null} after teardown.
     *
     * @return Vert.x instance or {@code null}
     */
    Vertx getVertxOrNull();

    /**
     * Returns the active cluster manager, or {@code null} after teardown.
     *
     * @return cluster manager or {@code null}
     */
    ClusterManager getClusterManager();

    /**
     * Returns the web-client configuration used for join and rejoin.
     *
     * @return configurations
     */
    Configurations getConfigurations();

    /**
     * Joins (or rejoins) the Vert.x cluster. Must be invoked while holding the lifecycle lock.
     *
     * @param configurations web-client configuration
     */
    void joinCluster(Configurations configurations);

    /**
     * Stops accepting membership events, unregisters listeners, and detaches Vert.x/WebClient.
     * Must be invoked while holding the lifecycle lock.
     *
     * @return detached {@link Vertx} to close outside the lock, or {@code null}
     */
    Vertx prepareTeardown();

    /**
     * Awaits Vert.x close completion outside the lifecycle lock.
     *
     * @param vertx detached Vert.x to close, or {@code null}
     */
    void awaitVertxClose(Vertx vertx);

    /**
     * Runs the supplier while holding the lifecycle lock.
     *
     * @param supplier action to run under the lock
     * @param <T>      result type
     * @return supplier result
     */
    <T> T withLifecycleLock(Supplier<T> supplier);

    /**
     * Runs the action while holding the lifecycle lock.
     *
     * @param action action to run under the lock
     */
    void runUnderLifecycleLock(Runnable action);
}
