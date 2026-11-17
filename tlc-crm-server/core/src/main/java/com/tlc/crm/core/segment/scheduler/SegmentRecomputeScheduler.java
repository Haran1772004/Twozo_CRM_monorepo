package com.tlc.crm.core.segment.scheduler;

import com.tlc.cluster.DistributedLock;
import com.tlc.cluster.service.ClusterService;
import com.tlc.cluster.service.provider.ClusterServiceProvider;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.segment.handler.OrganizationPageHandler;
import com.tlc.crm.core.segment.handler.SegmentPageHandler;
import com.tlc.crm.core.segment.service.SegmentRecomputeService;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Segment recalculation scheduler. Schedules periodic tasks that discover
 * active segments across organizations and enqueues recalculation requests
 * for each segment.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class SegmentRecomputeScheduler implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(SegmentRecomputeScheduler.class);

    private static final String THREAD_POOL_NAME = "segment-recompute-pool";
    private static final String TASK_SCHEDULER_NAME = "segment-recompute";
    private static final String DISTRIBUTED_LOCK_KEY = "segment-recompute-lock";

    private static final String CONFIG_INTERVAL_SECONDS = "segment.recompute.intervalSeconds";
    private static final long DEFAULT_INTERVAL_SECONDS = 1800L;

    private static final String CONFIG_RECOMPUTE_THRESHOLD_SECONDS = "segment.recompute.thresholdSeconds";
    private static final long DEFAULT_RECOMPUTE_THRESHOLD_SECONDS = 3600L;

    private static final String CONFIG_PAGE_SIZE = "segment.recompute.pageSize";
    private static final int DEFAULT_PAGE_SIZE = 100;

    private final SchedulerService schedulerService;
    private Long schedulerId;
    private SegmentRecomputeTask task;

    private SegmentRecomputeScheduler() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    private static final class InstanceHolder {
        private static final SegmentRecomputeScheduler INSTANCE = new SegmentRecomputeScheduler();
    }

    public static SegmentRecomputeScheduler getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Starts the scheduler, scheduling the recurring segment-recalculation
     * discovery task if it has not already been scheduled.
     * </p>
     *
     * @param configurations Represents the configuration source used to resolve
     *                       the recalculation interval.
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            final long intervalMillis = resolveIntervalMillis(configurations);
            final long recomputeThresholdMillis = resolveRecomputeThresholdMillis(configurations);
            final int pageSize = resolvePageSize(configurations);

            this.task = new SegmentRecomputeTask(new SegmentRecomputeController(intervalMillis), recomputeThresholdMillis, pageSize);
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, task);

            LOGGER.info("SegmentRecomputeScheduler started, Scheduler ID : {}, ThreadPool: {}, Interval: {}ms, Threshold: {}ms, PageSize: {}",
                    schedulerId, THREAD_POOL_NAME, intervalMillis, recomputeThresholdMillis, pageSize);
        }
    }

    /**
     * <p>
     * Resolves the recompute-staleness threshold, in milliseconds, from
     * configuration, falling back to {@link #DEFAULT_RECOMPUTE_THRESHOLD_SECONDS}
     * when unset or invalid.
     * </p>
     *
     * @param configurations Represents the configuration source.
     * @return the resolved threshold, in milliseconds.
     */
    private long resolveRecomputeThresholdMillis(final Configurations configurations) {
        long thresholdSeconds = DEFAULT_RECOMPUTE_THRESHOLD_SECONDS;

        if (Objects.nonNull(configurations)) {
            try {
                final long configuredSeconds = configurations.getLong(CONFIG_RECOMPUTE_THRESHOLD_SECONDS);

                if (configuredSeconds > 0) {
                    thresholdSeconds = configuredSeconds;
                }
            } catch (final Exception ignored) {
            }
        }
        return TimeUnit.SECONDS.toMillis(thresholdSeconds);
    }

    /**
     * <p>
     * Resolves the page size used when paging organizations and segments,
     * falling back to {@link #DEFAULT_PAGE_SIZE} when unset or invalid.
     * </p>
     *
     * @param configurations Represents the configuration source.
     * @return the resolved page size.
     */
    private int resolvePageSize(final Configurations configurations) {
        if (Objects.isNull(configurations)) {
            return DEFAULT_PAGE_SIZE;
        }

        try {
            return configurations.optInt(CONFIG_PAGE_SIZE, DEFAULT_PAGE_SIZE);
        } catch (final Exception ignored) {
            return DEFAULT_PAGE_SIZE;
        }
    }

    /**
     * <p>
     * Stops the scheduler, removing the scheduled task and stopping it if
     * currently running.
     * </p>
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId) && Objects.nonNull(schedulerService)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(task) && task.isRunning()) {
                task.stopTask();
            }
            LOGGER.info("SegmentRecomputeScheduler stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * Resolves the recalculation interval, in milliseconds, from configuration,
     * falling back to {@link #DEFAULT_INTERVAL_SECONDS} when unset or invalid.
     * </p>
     *
     * @param configurations Represents the configuration source.
     * @return the resolved interval, in milliseconds.
     */
    private long resolveIntervalMillis(final Configurations configurations) {
        long intervalSeconds = DEFAULT_INTERVAL_SECONDS;

        if (Objects.nonNull(configurations)) {
            try {
                final long configuredSeconds = configurations.getLong(CONFIG_INTERVAL_SECONDS);

                if (configuredSeconds > 0) {
                    intervalSeconds = configuredSeconds;
                }
            } catch (final Exception ignored) {
            }
        }
        return TimeUnit.SECONDS.toMillis(intervalSeconds);
    }

    /**
     * <p>
     * Task that, under a cluster-wide distributed lock, discovers segments
     * requiring recomputation and enqueues recompute events. Uses pagination
     * for both organizations and segments to avoid loading large datasets.
     * </p>
     */
    private static final class SegmentRecomputeTask extends Task implements SecureUserTaskProcessor {

        private final ClusterService clusterService;
        private final SegmentRecomputeService segmentRecomputeService;
        private final UserService userService;
        private final long recomputeThresholdMillis;
        private final int pageSize;

        private SegmentRecomputeTask(final TaskController taskController, final long recomputeThresholdMillis, final int pageSize) {
            super(taskController);
            this.clusterService = ClusterServiceProvider.get();
            this.segmentRecomputeService = SegmentRecomputeService.getInstance();
            this.recomputeThresholdMillis = recomputeThresholdMillis;
            this.userService = UserService.getInstance();
            this.pageSize = pageSize;
        }

        @Override
        public void execute(final TaskController taskController) {
            DistributedLock distributedLock = null;

            try {
                distributedLock = clusterService.getDistributedLock(DISTRIBUTED_LOCK_KEY);
                distributedLock.acquire();
                queueRecomputations();
            } catch (final Exception exception) {
                LOGGER.warn("SegmentRecalc task failed, message: {}", exception.getMessage(), exception);
            } finally {
                if (Objects.nonNull(distributedLock)) {
                    try {
                        distributedLock.release();
                    } catch (final Exception exception) {
                        LOGGER.warn("SegmentRecalc lock release failed, message: {}", exception.getMessage(), exception);
                    }
                }
            }
        }

        /**
         * <p>
         * Pages through all organizations and, for each, pages through and
         * queues its stale segments for recomputation.
         * </p>
         */
        private void queueRecomputations() {
            final long interval = Instant.now().toEpochMilli() - recomputeThresholdMillis;
            final OrganizationPageHandler orgPageHandler = OrganizationPageHandler.of(pageSize);

            try {
                do {
                    final Collection<CrmOrg> organizations = orgPageHandler.getCurrentPage().getData();

                    if (Objects.isNull(organizations) || organizations.isEmpty()) {
                        continue;
                    }

                    for (final CrmOrg org : organizations) {
                        final Long orgId = org.id();
                        final CrmUser adminUser = userService.getAdminUser(orgId);

                        try {
                            secureProcess(orgId, adminUser.id(), () -> {
                                processOrganization(orgId, interval);
                            });
                        } catch (final Throwable throwable) {
                            LOGGER.warn("Org segment queueing failed, orgId={}, message: {}", org.id(), throwable.getMessage(), throwable);
                        }
                    }
                } while (orgPageHandler.navigateToNextPageIfAvailable());
            } catch (final Throwable throwable) {
                LOGGER.warn("Failed while paging organizations, message: {}", throwable.getMessage(), throwable);
            }
        }

        /**
         * <p>
         * Pages through the stale segments for a single organization, publishing
         * a recompute event for each and marking it as calculated only once the
         * publishing succeeds.
         * </p>
         *
         * @param orgId    Represents the organization id.
         * @param interval Represents the last-calculated-at interval timestamp.
         */
        private void processOrganization(final long orgId, final long interval) {
            final SegmentPageHandler segmentPageHandler = SegmentPageHandler.of(orgId, interval, pageSize);

            try {
                do {
                    final Collection<Long> segmentIds = segmentPageHandler.getCurrentPage().getData();

                    if (Objects.isNull(segmentIds) || segmentIds.isEmpty()) {
                        continue;
                    }

                    for (final Long segmentId : segmentIds) {
                        try {
                            segmentRecomputeService.publish(orgId, segmentId);
                        } catch (final Throwable throwable) {
                            LOGGER.debug("Failed to queue segment recompute, org={}, segmentId={}, message: {}", orgId, segmentId, throwable.getMessage());
                        }
                    }
                } while (segmentPageHandler.navigateToNextPageIfAvailable());
            } catch (final Throwable throwable) {
                LOGGER.warn("Failed while paging segments for orgId={}, message: {}", orgId, throwable.getMessage(), throwable);
            }
        }
    }

    /**
     * <p>
     * Task controller providing the fixed recalculation-discovery interval.
     * </p>
     */
    public static final class SegmentRecomputeController extends TaskController {

        private final long intervalMillis;

        /**
         * <p>
         * Constructs a new {@link SegmentRecomputeController}.
         * </p>
         *
         * @param intervalMillis Represents the interval, in milliseconds, between task runs.
         */
        public SegmentRecomputeController(final long intervalMillis) {
            super(TASK_SCHEDULER_NAME);
            this.intervalMillis = intervalMillis;
        }

        /**
         * <p>
         * Gets the interval until the next scheduled run.
         * </p>
         *
         * @return the interval, in milliseconds.
         */
        @Override
        public long getNextInterval() {
            return intervalMillis;
        }
    }
}