package com.tlc.web.client.internal.cluster;

import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduler task that drives periodic health checks and in-JVM Vert.x cluster reboot attempts.
 *
 * @author sundar
 * @version 1.0
 */
final class VertxClusterRebootTask extends Task {

    private static final Logger LOGGER = LoggerFactory.getLogger(VertxClusterRebootTask.class);

    private final ClusterRecoveryCoordinator coordinator;

    /**
     * Creates a reboot task bound to the given recovery coordinator.
     *
     * @param coordinator recovery coordinator that owns reboot state
     * @param controller  interval controller for the scheduler
     */
    VertxClusterRebootTask(final ClusterRecoveryCoordinator coordinator, final TaskController controller) {
        super(controller);
        this.coordinator = coordinator;
    }

    /**
     * Runs a deferred reboot, an immediate reboot attempt, or a membership health check.
     *
     * @param taskController scheduler task controller (unused)
     */
    @Override
    public void execute(final TaskController taskController) {
        try {
            if (coordinator.isRebootRequested()) {
                final long deferRemainingMs = resolvePeerJoinRebootDeferMs();
                if (deferRemainingMs > 0L) {
                    coordinator.setNextIntervalMs(
                            Math.min(deferRemainingMs, ClusterRecoveryCoordinator.REBOOT_IDLE_INTERVAL_MS));
                    LOGGER.debug(
                            "Deferring Vert.x reboot for {} ms to allow peer handler registration", deferRemainingMs);
                    return;
                }
                coordinator.executeRebootAttempt();
            } else {
                coordinator.evaluateClusterHealth();
            }
        } catch (Exception exception) {
            LOGGER.error("Vert.x cluster reboot scheduler action failed", exception);
        }
    }

    /**
     * Returns remaining peer-join defer time, or {@code 0} when reboot should proceed.
     *
     * @return milliseconds to wait before reboot, or {@code 0} when deferral has elapsed
     */
    private long resolvePeerJoinRebootDeferMs() {
        final long rebootDueAtMs = coordinator.getPeerJoinRebootDueAtMs();
        if (rebootDueAtMs <= 0L) {
            return 0L;
        }
        final long remainingMs = rebootDueAtMs - System.currentTimeMillis();
        if (remainingMs > 0L) {
            return remainingMs;
        }
        coordinator.clearPeerJoinRebootDueAtMs();
        return 0L;
    }
}
