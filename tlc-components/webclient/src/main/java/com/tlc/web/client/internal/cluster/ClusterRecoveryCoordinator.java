package com.tlc.web.client.internal.cluster;

import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.web.client.internal.cluster.strategy.CuratorConnectionStrategy;
import com.tlc.web.client.internal.cluster.strategy.MembershipHealthStrategy;
import com.tlc.web.client.internal.cluster.strategy.PeerJoinRebootStrategy;
import io.vertx.core.Vertx;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Orchestrates in-JVM Vert.x cluster reboot: single-flight scheduling, backoff, health ticks,
 * and post-join resource restore. Policy lives in strategies; Vert.x close/join stays on
 * {@link VertxLifecycle}.
 *
 * <p>Thread-safety: membership/Curator callbacks and the scheduler thread coordinate via atomics
 * ({@code rebootRequested}, {@code acceptingMembershipEvents}, {@code connectionState}). Vert.x
 * teardown/join is serialized through {@link VertxLifecycle}'s lifecycle lock; blocking
 * {@code vertx.close()} runs outside the lock.</p>
 *
 * @author sundar
 * @version 1.0
 */
public final class ClusterRecoveryCoordinator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRecoveryCoordinator.class);

    private static final long REBOOT_INITIAL_DELAY_MS = 1_000L;
    private static final long REBOOT_MAX_DELAY_MS = 60_000L;
    static final long REBOOT_IDLE_INTERVAL_MS = 5_000L;
    private static final long PEER_JOIN_REBOOT_DELAY_MS = 3_000L;

    private static final String REBOOT_THREAD_POOL = "vertx-cluster-reboot";

    private final VertxLifecycle lifecycle;
    private final ClusterResourceRestorer restorer;
    private final CuratorConnectionStrategy curatorStrategy;
    private final MembershipHealthStrategy membershipHealthStrategy;
    private final PeerJoinRebootStrategy peerJoinRebootStrategy;

    private final AtomicBoolean rebootRequested = new AtomicBoolean(false);
    private final AtomicBoolean acceptingMembershipEvents = new AtomicBoolean(false);
    private final AtomicBoolean clusterUnhealthy = new AtomicBoolean(false);
    private final AtomicInteger maxObservedClusterSize = new AtomicInteger(1);
    private final AtomicLong peerJoinRebootDueAtMs = new AtomicLong(0L);
    private final AtomicLong nextIntervalMs = new AtomicLong(REBOOT_IDLE_INTERVAL_MS);
    private final AtomicInteger rebootAttempt = new AtomicInteger(0);
    private final AtomicReference<ClusterConnectionState> connectionState =
            new AtomicReference<>(ClusterConnectionState.HEALTHY);

    private volatile Long schedulerId;
    private volatile VertxClusterRebootTask rebootTask;

    /**
     * Creates a coordinator for the given Vert.x lifecycle port.
     *
     * @param lifecycle Vert.x lifecycle implementation
     */
    public ClusterRecoveryCoordinator(final VertxLifecycle lifecycle) {
        this.lifecycle = Objects.requireNonNull(lifecycle);
        this.restorer = new ClusterResourceRestorer();
        this.curatorStrategy = new CuratorConnectionStrategy(this);
        this.membershipHealthStrategy = new MembershipHealthStrategy(this);
        this.peerJoinRebootStrategy = new PeerJoinRebootStrategy(this);
    }

    /**
     * Returns the Vert.x lifecycle port used for lock-protected join and teardown.
     *
     * @return lifecycle port
     */
    public VertxLifecycle getLifecycle() {
        return lifecycle;
    }

    /**
     * Returns the membership health strategy (used by {@link com.tlc.web.client.internal.VertxClient} tests).
     *
     * @return membership health strategy
     */
    public MembershipHealthStrategy getMembershipHealthStrategy() {
        return membershipHealthStrategy;
    }

    /**
     * Resets coordinator state when {@link VertxLifecycle} starts.
     */
    public void resetForStart() {
        rebootRequested.set(false);
        maxObservedClusterSize.set(1);
        peerJoinRebootDueAtMs.set(0L);
        nextIntervalMs.set(REBOOT_IDLE_INTERVAL_MS);
        rebootAttempt.set(0);
        updateConnectionState(ClusterConnectionState.HEALTHY, false);
    }

    /**
     * Clears reboot and membership flags when {@link VertxLifecycle} stops.
     */
    public void onStop() {
        acceptingMembershipEvents.set(false);
        rebootRequested.set(false);
        updateConnectionState(ClusterConnectionState.HEALTHY, false);
    }

    /**
     * Installs membership and Curator monitoring after a successful cluster join.
     *
     * @param manager active cluster manager
     */
    public void installMonitoring(final ClusterManager manager) {
        peerJoinRebootStrategy.install(manager);
        curatorStrategy.install(manager);
    }

    /**
     * Removes the Curator connection-state listener during Vert.x teardown.
     */
    public void removeCuratorListener() {
        curatorStrategy.remove();
    }

    /**
     * Records the largest cluster size observed for routing readiness checks.
     *
     * @param zkManager ZooKeeper cluster manager
     */
    public void trackClusterSize(final ZookeeperClusterManager zkManager) {
        maxObservedClusterSize.updateAndGet(previous -> Math.max(previous, zkManager.getNodes().size()));
    }

    /**
     * Returns the largest cluster node count observed since start or last reset.
     *
     * @return maximum observed cluster size
     */
    public int getMaxObservedClusterSize() {
        return maxObservedClusterSize.get();
    }

    /**
     * Returns whether membership node events should trigger reboot policy.
     *
     * @return {@code true} when membership listeners are armed
     */
    public boolean isAcceptingMembershipEvents() {
        return acceptingMembershipEvents.get();
    }

    /**
     * Arms or disarms membership node listeners.
     *
     * @param accepting {@code true} to accept membership events
     */
    public void setAcceptingMembershipEvents(final boolean accepting) {
        acceptingMembershipEvents.set(accepting);
    }

    /**
     * Returns whether an in-JVM reboot is pending or in progress.
     *
     * @return {@code true} when reboot has been requested
     */
    public boolean isRebootRequested() {
        return rebootRequested.get();
    }

    /**
     * Returns whether the cluster is marked unhealthy for traffic gating.
     *
     * @return {@code true} when clustered traffic should be blocked
     */
    public boolean isClusterUnhealthy() {
        return clusterUnhealthy.get();
    }

    /**
     * Returns whether membership is unstable enough to warrant reboot on peer departure.
     *
     * @return {@code true} when membership is unstable
     */
    public boolean isClusterMembershipUnstable() {
        return membershipHealthStrategy.isClusterMembershipUnstable();
    }

    /**
     * Marks the cluster unhealthy after Curator session loss or suspension.
     */
    public void markUnhealthy() {
        updateConnectionState(ClusterConnectionState.UNHEALTHY, true);
    }

    /**
     * Marks the cluster as waiting for peer visibility after a successful rejoin.
     */
    public void markAwaitingPeers() {
        updateConnectionState(ClusterConnectionState.AWAITING_PEERS, true);
    }

    /**
     * Clears the unhealthy flag after routing becomes ready.
     */
    public void clearUnhealthy() {
        updateConnectionState(ClusterConnectionState.READY, false);
    }

    /**
     * Returns the current recovery connection state.
     *
     * @return connection state
     */
    public ClusterConnectionState getConnectionState() {
        return connectionState.get();
    }

    /**
     * Returns the scheduler interval until the next reboot task tick.
     *
     * @return next interval in milliseconds
     */
    public long getNextIntervalMs() {
        return nextIntervalMs.get();
    }

    /**
     * Sets the scheduler interval until the next reboot task tick.
     *
     * @param intervalMs next interval in milliseconds
     */
    void setNextIntervalMs(final long intervalMs) {
        nextIntervalMs.set(intervalMs);
    }

    /**
     * Returns the scheduled peer-join reboot defer deadline, or {@code 0} when not deferred.
     *
     * @return epoch milliseconds when peer-join reboot is due, or {@code 0}
     */
    long getPeerJoinRebootDueAtMs() {
        return peerJoinRebootDueAtMs.get();
    }

    /**
     * Clears the peer-join reboot defer deadline after deferral elapses.
     */
    void clearPeerJoinRebootDueAtMs() {
        peerJoinRebootDueAtMs.set(0L);
    }

    /**
     * Returns the current reboot attempt counter.
     *
     * @return reboot attempt number
     */
    public int getRebootAttempt() {
        return rebootAttempt.get();
    }

    /**
     * Sets the reboot attempt counter (test hook).
     *
     * @param attempt reboot attempt number
     */
    public void setRebootAttempt(final int attempt) {
        rebootAttempt.set(attempt);
    }

    /**
     * Returns the scheduler task identifier, or {@code null} when not registered.
     *
     * @return scheduler task id
     */
    public Long getSchedulerId() {
        return schedulerId;
    }

    /**
     * Registers the reboot task with {@link SchedulerService} once (idempotent).
     * Must be invoked while holding the Vert.x lifecycle lock.
     */
    public void registerScheduler() {
        final SchedulerService schedulerService = lookupSchedulerService();

        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            rebootTask = new VertxClusterRebootTask(this, new VertxClusterRebootController(this));
            schedulerId = schedulerService.schedule(REBOOT_THREAD_POOL, rebootTask);
            LOGGER.info("Vert.x cluster reboot scheduler started, Scheduler ID : {}", schedulerId);
        } else if (Objects.isNull(schedulerService)) {
            LOGGER.warn("SchedulerService is unavailable; Vert.x cluster reboot will not be scheduled");
        }
    }

    /**
     * Removes the reboot task from {@link SchedulerService} once (idempotent).
     * Must be invoked while holding the Vert.x lifecycle lock.
     */
    public void unregisterScheduler() {
        if (Objects.nonNull(schedulerId)) {
            final SchedulerService schedulerService = lookupSchedulerService();

            if (Objects.nonNull(schedulerService)) {
                schedulerService.removeTask(schedulerId);
                LOGGER.info("Vert.x cluster reboot scheduler stopped, Scheduler ID : {}", schedulerId);
            }

            if (Objects.nonNull(rebootTask) && rebootTask.isRunning()) {
                rebootTask.stopTask();
            }

            schedulerId = null;
            rebootTask = null;
        }
    }

    /**
     * Marks reboot as required (single-flight) and asks the scheduler to run the task ASAP.
     * Safe to call from Curator or membership callbacks; does not acquire the lifecycle lock
     * except briefly to read the scheduler id.
     */
    public void requestReboot() {
        if (lifecycle.isStopped()) {
            return;
        }
        if (!rebootRequested.compareAndSet(false, true)) {
            LOGGER.info("Vert.x cluster reboot already requested; ignoring duplicate membership event");
            return;
        }

        connectionState.set(ClusterConnectionState.REBOOTING);
        rebootAttempt.set(0);
        nextIntervalMs.set(REBOOT_INITIAL_DELAY_MS);
        runSchedulerNowIfRegistered();
    }

    /**
     * Defers reboot briefly so peer event consumers can finish handler registration before routing refresh.
     */
    public void requestRebootAfterPeerJoin() {
        peerJoinRebootDueAtMs.set(System.currentTimeMillis() + PEER_JOIN_REBOOT_DELAY_MS);
        markUnhealthy();
        requestReboot();
    }

    /**
     * Arms reboot state without scheduling an immediate run (test hook).
     */
    public void markRebootRequestedWithoutRunNow() {
        rebootRequested.set(true);
        rebootAttempt.set(0);
        nextIntervalMs.set(REBOOT_INITIAL_DELAY_MS);
        connectionState.set(ClusterConnectionState.REBOOTING);
    }

    /**
     * Delegates a periodic membership and routing health evaluation to the strategy.
     */
    public void evaluateClusterHealth() {
        membershipHealthStrategy.evaluate();
    }

    /**
     * Advances exponential backoff for the next reboot retry after a failed rejoin.
     */
    public void advanceRebootBackoff() {
        final int attempt = Math.max(rebootAttempt.get(), 1);
        final int shift = Math.min(attempt - 1, 16);
        final long delay = Math.min(REBOOT_INITIAL_DELAY_MS << shift, REBOOT_MAX_DELAY_MS);

        nextIntervalMs.set(delay);
        LOGGER.info("Vert.x cluster reboot next retry in {} ms", delay);
    }

    /**
     * Performs one reboot attempt: tear down if needed, rejoin, restore codecs/listeners.
     * Invoked from the scheduler thread only.
     */
    public void executeRebootAttempt() {
        if (lifecycle.isStopped() || !rebootRequested.get()) {
            return;
        }

        final Vertx teardownClose = lifecycle.withLifecycleLock(this::teardownForReboot);
        lifecycle.awaitVertxClose(teardownClose);

        if (lifecycle.isStopped() || !rebootRequested.get()) {
            return;
        }

        final AtomicReference<Vertx> joinFailureClose = new AtomicReference<>();
        lifecycle.runUnderLifecycleLock(() -> rejoinAfterTeardown(joinFailureClose));
        lifecycle.awaitVertxClose(joinFailureClose.get());
    }

    /**
     * Whether clustered event-bus traffic should be sent (excludes recovery/reboot windows).
     *
     * @return {@code true} when clustered traffic is allowed
     */
    public boolean isClusterReadyForTraffic() {
        if (lifecycle.isStopped() || rebootRequested.get() || clusterUnhealthy.get()) {
            return false;
        }
        return membershipHealthStrategy.isClusterRoutingReady();
    }

    /**
     * Detaches Vert.x under the lifecycle lock and prepares consumers for teardown.
     *
     * @return detached {@link Vertx} to close outside the lock, or {@code null}
     */
    private Vertx teardownForReboot() {
        if (lifecycle.isStopped() || !rebootRequested.get()) {
            return null;
        }
        if (Objects.isNull(lifecycle.getVertxOrNull())) {
            return null;
        }

        LOGGER.warn("Starting in-JVM Vert.x cluster reboot");
        acceptingMembershipEvents.set(false);
        membershipHealthStrategy.resetPeerStability();
        connectionState.set(ClusterConnectionState.REBOOTING);
        restorer.discardConsumersForTeardown();
        return lifecycle.prepareTeardown();
    }

    /**
     * Rejoins the cluster under the lifecycle lock after teardown.
     *
     * <p>Holding the lifecycle lock during {@code joinCluster} can delay {@code stop()} until the
     * lock is released; {@code stop()} uses a timed {@code tryLock} to avoid indefinite deadlock.</p>
     *
     * @param joinFailureClose receives a detached {@link Vertx} when rejoin fails
     */
    private void rejoinAfterTeardown(final AtomicReference<Vertx> joinFailureClose) {
        if (lifecycle.isStopped() || !rebootRequested.get()) {
            return;
        }

        try {
            final int attempt = rebootAttempt.incrementAndGet();
            LOGGER.info("Vert.x cluster reboot attempt {}", attempt);
            if (lifecycle.isStopped() || !rebootRequested.get()) {
                return;
            }
            lifecycle.joinCluster(lifecycle.getConfigurations());
            restorer.restoreAfterJoin();
            completeRebootSuccess(attempt);
        } catch (Exception exception) {
            handleRebootFailure(joinFailureClose, exception);
        }
    }

    /**
     * Clears reboot flags and sets READY or AWAITING_PEERS after a successful rejoin.
     *
     * @param attempt reboot attempt number that succeeded
     */
    private void completeRebootSuccess(final int attempt) {
        rebootRequested.set(false);
        rebootAttempt.set(0);
        nextIntervalMs.set(REBOOT_IDLE_INTERVAL_MS);
        peerJoinRebootDueAtMs.set(0L);
        membershipHealthStrategy.logMembershipAfterRejoin();
        if (membershipHealthStrategy.isClusterRoutingReady()) {
            updateConnectionState(ClusterConnectionState.READY, false);
            LOGGER.info("Vert.x cluster reboot succeeded after {} attempt(s)", attempt);
        } else {
            updateConnectionState(ClusterConnectionState.AWAITING_PEERS, true);
            LOGGER.info(
                    "Vert.x cluster reboot completed after {} attempt(s) but routing is not ready yet; waiting for peers",
                    attempt);
        }
    }

    /**
     * Discards consumers, tears down a failed join, and schedules the next backoff retry.
     *
     * @param joinFailureClose receives detached {@link Vertx} when rejoin fails
     * @param exception        failure from rejoin
     */
    private void handleRebootFailure(final AtomicReference<Vertx> joinFailureClose, final Exception exception) {
        LOGGER.error("Vert.x cluster reboot attempt failed: {}", exception.getMessage(), exception);
        acceptingMembershipEvents.set(false);
        restorer.discardConsumersForTeardown();
        joinFailureClose.set(lifecycle.prepareTeardown());
        advanceRebootBackoff();
    }

    /**
     * Triggers an immediate scheduler run when a reboot task is registered.
     */
    private void runSchedulerNowIfRegistered() {
        final Long id = lifecycle.withLifecycleLock(() -> schedulerId);
        final SchedulerService schedulerService = lookupSchedulerService();
        if (Objects.nonNull(schedulerService) && Objects.nonNull(id)) {
            schedulerService.runNow(id);
        }
    }

    /**
     * Updates connection state and the traffic-gating unhealthy flag together.
     *
     * @param state     new connection state
     * @param unhealthy whether clustered traffic should be gated
     */
    private void updateConnectionState(final ClusterConnectionState state, final boolean unhealthy) {
        connectionState.set(state);
        clusterUnhealthy.set(unhealthy);
    }

    /**
     * Looks up {@link SchedulerService} from the provider, returning {@code null} when unavailable.
     *
     * @return scheduler service, or {@code null}
     */
    private static SchedulerService lookupSchedulerService() {
        try {
            return SchedulerServiceProvider.get();
        } catch (RuntimeException exception) {
            LOGGER.debug("SchedulerService lookup failed: {}", exception.getMessage());
            return null;
        }
    }
}
