package com.tlc.crm.core.api.insights.scheduler;

import com.tlc.cluster.DistributedLock;
import com.tlc.cluster.service.ClusterService;
import com.tlc.cluster.service.provider.ClusterServiceProvider;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.core.api.insights.models.EntityInsightsGroup;
import com.tlc.crm.core.api.insights.models.EntityInsightsRecomputeRequest;
import com.tlc.crm.core.api.insights.manager.EntityInsightsRecomputeRequestManager;
import com.tlc.crm.core.api.insights.models.EntityInsightsIdentifierType;
import com.tlc.crm.core.api.insights.service.ActivityInsightsAggregator;
import com.tlc.crm.core.api.insights.service.DealInsightsAggregator;
import com.tlc.crm.core.api.insights.service.EmailInsightsAggregator;
import com.tlc.crm.core.api.insights.service.EntityInsightsGroupAggregator;
import com.tlc.crm.core.api.insights.service.EntityInsightsService;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Scheduled {@link Service} that drains the {@code OMCRMEntityInsightsRecomputeRequest}
 * queue and dispatches each drained row to the matching aggregator.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class EntityInsightsRecomputeScheduler implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityInsightsRecomputeScheduler.class);

    private static final String THREAD_POOL_NAME = "entity-insights-recompute-pool";
    private static final String TASK_SCHEDULER_NAME = "entity-insights-recompute";
    private static final String DISTRIBUTED_LOCK_KEY = "entity-insights-recompute";

    private static final String CONFIG_INTERVAL_SECONDS = "entity.insights.recompute.intervalSeconds";
    private static final long DEFAULT_INTERVAL_SECONDS = 10L;

    private static final String CONFIG_DRAIN_LIMIT = "entity.insights.recompute.drainLimit";
    private static final int DEFAULT_CONFIG_DRAIN_LIMIT = 100;

    private final SchedulerService schedulerService;
    private Long schedulerId;
    private EntityInsightsRecomputeTask updaterTask;

    private EntityInsightsRecomputeScheduler() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    private static final class InstanceHolder {
        private static final EntityInsightsRecomputeScheduler INSTANCE = new EntityInsightsRecomputeScheduler();
    }

    /**
     * <p>
     * Gets the {@link EntityInsightsRecomputeScheduler} singleton.
     * </p>
     *
     * @return The {@link EntityInsightsRecomputeScheduler} instance.
     */
    public static EntityInsightsRecomputeScheduler getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            final long intervalMillis = resolveIntervalMillis(configurations);
            final int drainLimit = resolveDrainLimit(configurations);

            this.updaterTask = new EntityInsightsRecomputeTask(
                    new EntityInsightsRecomputeController(intervalMillis), drainLimit);
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, updaterTask);

            LOGGER.info("EntityInsightsRecomputeScheduler started, Scheduler ID : {}, Interval: {}ms, DrainLimit: {}",
                    schedulerId, intervalMillis, drainLimit);
        }
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId) && Objects.nonNull(schedulerService)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(updaterTask) && updaterTask.isRunning()) {
                updaterTask.stopTask();
            }
            LOGGER.info("EntityInsightsRecomputeScheduler stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * Resolves the interval in seconds.
     * </p>
     *
     * @param configurations Represents the start-time configurations bundle.
     * @return The interval in milliseconds.
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
     * Resolves the drain limit from configuration,
     * falling back to {@link #DEFAULT_CONFIG_DRAIN_LIMIT}.
     * </p>
     *
     * @param configurations Represents the start-time configurations bundle.
     * @return The drain limit.
     */
    private int resolveDrainLimit(final Configurations configurations) {
        if (Objects.nonNull(configurations)) {
            final int configuredLimit = configurations.optInt(CONFIG_DRAIN_LIMIT, DEFAULT_CONFIG_DRAIN_LIMIT);

            if (configuredLimit > 0) {
                return configuredLimit;
            }
        }

        return DEFAULT_CONFIG_DRAIN_LIMIT;
    }

    /**
     * <p>
     * Represents the entity insights recompute task
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    private static final class EntityInsightsRecomputeTask extends Task {

        private final ClusterService clusterService;
        private final EntityInsightsRecomputeRequestManager entityInsightsRecomputeRequestManager;
        private final EntityInsightsService entityInsightsService;
        private final int drainLimit;

        private EntityInsightsRecomputeTask(final TaskController taskController, final int drainLimit) {
            super(taskController);
            this.clusterService = ClusterServiceProvider.get();
            this.entityInsightsRecomputeRequestManager = EntityInsightsRecomputeRequestManager.getInstance();
            this.entityInsightsService = EntityInsightsService.getInstance();
            this.drainLimit = drainLimit;
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         */
        @Override
        public void execute(final TaskController taskController) {
            DistributedLock distributedLock = null;

            try {
                distributedLock = clusterService.getDistributedLock(DISTRIBUTED_LOCK_KEY);
                distributedLock.acquire();
                recomputeEntityInsights();
            } catch (final Exception exception) {
                LOGGER.warn("EntityInsights recompute action failed, message: {}", exception.getMessage(), exception);
            } finally {
                if (Objects.nonNull(distributedLock)) {
                    try {
                        distributedLock.release();
                    } catch (final Exception exception) {
                        LOGGER.warn("EntityInsights recompute distributed lock release action failed, message: {}", exception.getMessage(), exception);
                    }
                }
            }
        }

        /**
         * <p>
         * Fetches the total pending record count first, then iterates through
         * all records in {@code drainLimit}-sized batches until every record
         * has been processed.
         * </p>
         */
        private void recomputeEntityInsights() {
            final long totalCount = entityInsightsRecomputeRequestManager.getTotalCount();

            if (totalCount == 0) {
                return;
            }
            LOGGER.debug("EntityInsights recompute started, totalPending={}, batchSize={}", totalCount, drainLimit);

            for (long index = 0; index < totalCount; index += drainLimit) {
                final Collection<EntityInsightsRecomputeRequest> drainedEntries =
                        entityInsightsRecomputeRequestManager.getEntityInsightsRequest(drainLimit);

                if (drainedEntries.isEmpty()) {
                    break;
                }
                final Map<Long, Map<EntityInsightsGroup, Map<Source, Collection<Long>>>> entityBuckets =
                        groupEntityEntries(drainedEntries);
                final Map<Long, Map<EntityInsightsGroup, Map<Source, Collection<Long>>>> sourceRefBuckets =
                        groupSourceRefEntries(drainedEntries);
                final Map<Long, Collection<Long>> queueRowIdsByOrg =
                        groupQueueRowIdsByOrg(drainedEntries);
                final Collection<Long> orgIds = new HashSet<>();

                orgIds.addAll(entityBuckets.keySet());
                orgIds.addAll(sourceRefBuckets.keySet());

                for (final Long orgId : orgIds) {
                    final Map<EntityInsightsGroup, Map<Source, Collection<Long>>> entityByGroup =
                            entityBuckets.getOrDefault(orgId, Map.of());
                    final Map<EntityInsightsGroup, Map<Source, Collection<Long>>> sourceRefByGroup =
                            sourceRefBuckets.getOrDefault(orgId, Map.of());
                    final Collection<Long> rowIds = queueRowIdsByOrg.getOrDefault(orgId, List.of());

                    dispatchOrgBucket(orgId, entityByGroup, sourceRefByGroup, rowIds);
                }
            }
        }

        /**
         * <p>
         * Runs the aggregations and the queue cleanup for one org bucket.
         * </p>
         *
         * @param orgId         Represents the organization id.
         * @param entityByGroup Represents already-resolved entity buckets per group / source.
         * @param refsByGroup   Represents source-ref buckets per group / source.
         * @param rowIds        Represents the queue-row primary keys to clear after success.
         */
        private void dispatchOrgBucket(final Long orgId, final Map<EntityInsightsGroup, Map<Source, Collection<Long>>> entityByGroup,
                                       final Map<EntityInsightsGroup, Map<Source, Collection<Long>>> refsByGroup, final Collection<Long> rowIds) {
            try {
                entityByGroup.forEach((group, bySource) -> recomputeFor(group).aggregate(orgId, bySource));

                refsByGroup.forEach((group, byRefSource) ->
                        byRefSource.forEach((refSource, sourceKeys) ->
                                entityInsightsService.processRelatedEntity(orgId, group, refSource, sourceKeys,
                                        (targetSource, batch) -> recomputeFor(group).aggregate(orgId, targetSource, batch))));

                entityInsightsRecomputeRequestManager.remove(orgId, rowIds);
            } catch (final Throwable throwable) {
                LOGGER.warn("Org entity insights recompute action failed: org={}", orgId, throwable);
            }
        }

        /**
         * <p>
         * Groups already-resolved {@code ENTITY}-payload entries by
         * {@code (orgId, group, source)}.
         * </p>
         *
         * @param drainedEntries Represents the entries to group.
         * @return The nested grouping limited to {@code ENTITY} payload rows.
         */
        private Map<Long, Map<EntityInsightsGroup, Map<Source, Collection<Long>>>> groupEntityEntries(
                final Collection<EntityInsightsRecomputeRequest> drainedEntries) {
            return groupData(drainedEntries, EntityInsightsIdentifierType.ENTITY);
        }

        /**
         * <p>
         * Groups {@code SOURCE_REF}-payload entries by {@code (orgId, group, refSource)};
         * the inner {@link Source} is the source-table type of the keys, not the target entity type.
         * </p>
         *
         * @param drainedEntries Represents the entries to group.
         * @return The nested grouping limited to {@code SOURCE_REF} payload rows.
         */
        private Map<Long, Map<EntityInsightsGroup, Map<Source, Collection<Long>>>> groupSourceRefEntries(
                final Collection<EntityInsightsRecomputeRequest> drainedEntries) {
            return groupData(drainedEntries, EntityInsightsIdentifierType.SOURCE_REF);
        }

        /**
         * <p>
         * Shared grouping helper that buckets entries of one payload kind by
         * {@code (orgId, group, source)}.
         * </p>
         *
         * @param drainedEntries Represents the entries to group.
         * @param payload        Represents the payload kind to keep.
         * @return The nested grouping limited to the given payload.
         */
        private Map<Long, Map<EntityInsightsGroup, Map<Source, Collection<Long>>>> groupData(
                final Collection<EntityInsightsRecomputeRequest> drainedEntries,
                final EntityInsightsIdentifierType payload) {
            final Map<Long, Map<EntityInsightsGroup, Map<Source, Collection<Long>>>> grouped = new HashMap<>();

            for (final EntityInsightsRecomputeRequest entry : drainedEntries) {
                if (entry.payload() != payload) {
                    continue;
                }

                grouped.computeIfAbsent(entry.orgId(), key -> new HashMap<>())
                        .computeIfAbsent(entry.group(), key -> new EnumMap<>(Source.class))
                        .computeIfAbsent(entry.source(), key -> new HashSet<>())
                        .add(entry.entityId());
            }
            return grouped;
        }

        /**
         * <p>
         * Groups drained-row primary keys by {@code orgId} so the per-org
         * delete can be issued through the org's data store inside the same secured context.
         * </p>
         *
         * @param drainedEntries Represents the entries that were drained.
         * @return The queue-row ids partitioned by organization id.
         */
        private Map<Long, Collection<Long>> groupQueueRowIdsByOrg(final Collection<EntityInsightsRecomputeRequest> drainedEntries) {
            final Map<Long, Collection<Long>> queueRowIdsByOrg = new HashMap<>();

            for (final EntityInsightsRecomputeRequest entry : drainedEntries) {
                queueRowIdsByOrg.computeIfAbsent(entry.orgId(), key -> new ArrayList<>()).add(entry.id());
            }
            return queueRowIdsByOrg;
        }

        /**
         * <p>
         * Resolves the aggregator for the given group.
         * </p>
         *
         * @param group Represents the group.
         * @return The matching {@link EntityInsightsGroupAggregator}.
         */
        private EntityInsightsGroupAggregator recomputeFor(final EntityInsightsGroup group) {
            return switch (group) {
                case ACTIVITY -> ActivityInsightsAggregator.getInstance();
                case EMAIL -> EmailInsightsAggregator.getInstance();
                case DEAL -> DealInsightsAggregator.getInstance();
            };
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         */
        @Override
        public boolean stopTask() {
            return true;
        }
    }

    /**
     * <p>
     * Represents the entity insights recompute controller.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public static final class EntityInsightsRecomputeController extends TaskController {

        private final long intervalMillis;

        /**
         * <p>
         * Constructs a new controller.
         * </p>
         *
         * @param intervalMillis Represents the interval in milliseconds.
         */
        public EntityInsightsRecomputeController(final long intervalMillis) {
            super(TASK_SCHEDULER_NAME);
            this.intervalMillis = intervalMillis;
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         */
        @Override
        public long getNextInterval() {
            return intervalMillis;
        }
    }
}