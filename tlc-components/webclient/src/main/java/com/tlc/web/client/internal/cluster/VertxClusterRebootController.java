package com.tlc.web.client.internal.cluster;

import com.tlc.scheduler.task.TaskController;

/**
 * Supplies the scheduler interval for the Vert.x cluster reboot task.
 *
 * @author sundar
 * @version 1.0
 */
final class VertxClusterRebootController extends TaskController {

    private static final String SCHEDULER_NAME = "vertx_cluster_reboot";

    private final ClusterRecoveryCoordinator coordinator;

    /**
     * Creates a controller bound to the given recovery coordinator.
     *
     * @param coordinator recovery coordinator that owns interval state
     */
    VertxClusterRebootController(final ClusterRecoveryCoordinator coordinator) {
        super(SCHEDULER_NAME);
        this.coordinator = coordinator;
    }

    /**
     * Returns the delay until the next scheduler tick.
     *
     * @return next interval in milliseconds
     */
    @Override
    public long getNextInterval() {
        return coordinator.getNextIntervalMs();
    }
}
