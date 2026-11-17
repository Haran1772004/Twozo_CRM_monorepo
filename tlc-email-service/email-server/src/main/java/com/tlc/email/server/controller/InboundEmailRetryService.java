package com.tlc.email.server.controller;

import com.tlc.attachment.storage.model.ObjectCountRequest;
import com.tlc.attachment.storage.model.ObjectListRequest;
import com.tlc.attachment.storage.model.ObjectListResponse;
import com.tlc.attachment.storage.model.ObjectSummary;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.email.server.internal.publisher.EmailEventPublisher;
import com.tlc.email.server.internal.storage.InboundEmailPayloadBuilder;
import com.tlc.email.server.internal.storage.InboundEmailProcessingTracker;
import com.tlc.email.server.internal.storage.InboundEmailStorageService;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Handles retry processing for inbound email objects that are pending delivery to the
 * {@code ReceiveEmailAction} endpoint. Republishes eligible objects so they can be reprocessed;
 * the objects themselves are left in storage and are only removed once the downstream listener
 * successfully processes them.
 * </p>
 *
 * @author sundar
 * @version 1.0
 */
public final class InboundEmailRetryService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(InboundEmailRetryService.class);

    private static final String THREAD_POOL_NAME = "inbound-email-retry-pool";
    private static final String TASK_SCHEDULER_NAME = "inbound-email-retry";
    private static final String TASK_SCHEDULE_DELAY = "inbound.retry.schedule.delay";
    private static final String OBJECT_PREFIX = "inbound.retry.object.prefix";

    private final SchedulerService schedulerService;
    private Long schedulerId;
    private InboundEmailRetryTask inboundEmailRetryTask;

    private InboundEmailRetryService() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    private static class Instance {
        private static final InboundEmailRetryService INSTANCE = new InboundEmailRetryService();
    }

    /**
     * <p>
     * Returns the singleton instance of the inbound email retry service.
     * </p>
     *
     * @return singleton instance of {@link InboundEmailRetryService}
     */
    public static InboundEmailRetryService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Starts the inbound email retry service and schedules the retry task.
     * </p>
     *
     * @param configurations service configurations
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            this.inboundEmailRetryTask = new InboundEmailRetryTask(new InboundEmailRetryTaskController(configurations));
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, inboundEmailRetryTask);
            LOGGER.info("Inbound Email Retry: Service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * Stops the inbound email retry service and removes the scheduled retry task.
     * </p>
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId) && Objects.nonNull(schedulerService)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(inboundEmailRetryTask) && inboundEmailRetryTask.isRunning()) {
                inboundEmailRetryTask.stopTask();
            }

            LOGGER.info("Inbound Email Retry: Service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    private static class InboundEmailRetryTask extends Task {

        private final InboundEmailStorageService inboundEmailStorageService;
        private final EmailEventPublisher emailEventPublisher;
        private final InboundEmailProcessingTracker processingTracker;
        private final String objectPrefix;

        private InboundEmailRetryTask(final TaskController controller) {
            super(controller);
            this.inboundEmailStorageService = InboundEmailStorageService.getInstance();
            this.emailEventPublisher = EmailEventPublisher.get();
            this.processingTracker = InboundEmailProcessingTracker.getInstance();
            final Configurations configurations = ((InboundEmailRetryTaskController) controller).configurations;
            this.objectPrefix = configurations.optConfig(OBJECT_PREFIX, "");
        }

        /**
         * <p>
         * Executes the inbound email retry task by finding objects pending delivery and
         * republishing them for processing.
         * </p>
         *
         * @param taskController controller for the scheduled task
         */
        @Override
        public void execute(final TaskController taskController) {
            final Map<String, String> metadataFilter = inboundEmailStorageService.getSendRequestedMetadataFilter();
            final ObjectCountRequest countRequest = new ObjectCountRequest(objectPrefix, metadataFilter);

            inboundEmailStorageService.getObjectStorageService()
                    .countObjectsAsync(countRequest)
                    .thenAccept(totalCount -> processBatches(totalCount, metadataFilter))
                    .exceptionally(throwable -> {
                        LoggerUtil.logError(LOGGER, throwable,
                                "Inbound Email Retry: Count action failed. message={}, trace=",
                                throwable.getMessage(), throwable);
                        return null;
                    });
        }

        /**
         * <p>
         * Processes the available inbound email objects in batches.
         * </p>
         *
         * @param totalCount     total number of objects pending retry
         * @param metadataFilter metadata filter used to identify retry objects
         */
        private void processBatches(final Long totalCount, final Map<String, String> metadataFilter) {
            if (Objects.isNull(totalCount) || totalCount <= 0) {
                LOGGER.info("Inbound Email Retry: No objects with state=send_requested found at {}", Instant.now().toEpochMilli());
                return;
            }

            LOGGER.info("Inbound Email Retry: Found {} object(s) with state=send_requested", totalCount);
            republishNextBatch(metadataFilter, null, totalCount, 0L, new HashSet<>());
        }

        /**
         * <p>
         * Retrieves and republishes the next batch of inbound email objects.
         * </p>
         *
         * @param metadataFilter    metadata filter used to identify retry objects
         * @param continuationToken token used to retrieve the next batch
         * @param totalCount        total number of objects pending retry
         * @param processedCount    number of objects already processed
         * @param publishedThisRun  object identifiers already published during this run
         */
        private void republishNextBatch(final Map<String, String> metadataFilter,
                                        final String continuationToken, final long totalCount,
                                        final long processedCount, final Set<String> publishedThisRun) {
            if (processedCount >= totalCount) {
                LOGGER.info("Inbound Email Retry: Completed republish for {} object(s)", totalCount);
                return;
            }

            final ObjectListRequest listRequest = new ObjectListRequest(
                    objectPrefix,
                    continuationToken,
                    metadataFilter,
                    ObjectListRequest.MAX_KEYS_LIMIT
            );

            inboundEmailStorageService.getObjectStorageService()
                    .listObjectsAsync(listRequest)
                    .thenAccept(batch -> handleBatch(batch, metadataFilter, totalCount, processedCount, publishedThisRun))
                    .exceptionally(throwable -> {
                        LoggerUtil.logError(LOGGER, throwable,
                                "Inbound Email Retry: List action failed. message={}, trace=",
                                throwable.getMessage(), throwable);
                        return null;
                    });
        }

        /**
         * <p>
         * Processes a batch of inbound email objects and republishes eligible objects.
         * </p>
         *
         * @param batch            batch of inbound email objects
         * @param metadataFilter   metadata filter used to identify retry objects
         * @param totalCount       total number of objects pending retry
         * @param processedCount   number of objects already processed
         * @param publishedThisRun object identifiers already published during this run
         */
        private void handleBatch(final ObjectListResponse batch,
                                 final Map<String, String> metadataFilter, final long totalCount,
                                 final long processedCount, final Set<String> publishedThisRun) {
            final List<ObjectSummary> summaries = batch.summaries();
            long batchProcessed = 0;

            for (final ObjectSummary summary : summaries) {
                final String objectId = summary.key();

                if (!publishedThisRun.add(objectId) || !processingTracker.tryAcquire(objectId)) {
                    LOGGER.debug("Inbound Email Retry: Skipping duplicate or in-flight objectId={}", objectId);
                    batchProcessed++;
                    continue;
                }

                republish(objectId, summary);
                batchProcessed++;
            }
            final long nextProcessedCount = processedCount + batchProcessed;

            if (batch.truncated() && Objects.nonNull(batch.nextContinuationToken()) && nextProcessedCount < totalCount) {
                republishNextBatch(metadataFilter, batch.nextContinuationToken(), totalCount, nextProcessedCount, publishedThisRun);
                return;
            }

            LOGGER.info("Inbound Email Retry: Completed republish for {} object(s)", nextProcessedCount);
        }

        /**
         * <p>
         * Republishes the inbound email event for the given object. The object itself is left in
         * storage - it is not deleted here. Deletion happens downstream once the listener
         * successfully processes the republished event.
         * </p>
         *
         * @param objectId object identifier
         * @param summary  summary of the inbound email object
         */
        private void republish(final String objectId, final ObjectSummary summary) {
            final String bucketName = inboundEmailStorageService.getBucketName();
            final String payload = InboundEmailPayloadBuilder.buildPayload(bucketName, summary);

            try {
                emailEventPublisher.publishInboundEmailReceived(payload);
                LOGGER.info("Inbound Email Retry: Republished objectId={}", objectId);
            } catch (final Exception e) {
                LoggerUtil.logError(LOGGER, e,
                        "Inbound Email Retry: Republish failed for objectId={}, message={}, trace=",
                        objectId, e.getMessage(), e);
            } finally {
                processingTracker.release(objectId);
            }
        }

        /**
         * <p>
         * Stops the inbound email retry task.
         * </p>
         *
         * @return {@code true} when the task is stopped successfully
         */
        @Override
        public boolean stopTask() {
            return true;
        }
    }

    private static class InboundEmailRetryTaskController extends TaskController {

        private final Configurations configurations;
        private final long delay;

        private InboundEmailRetryTaskController(final Configurations configurations) {
            super(TASK_SCHEDULER_NAME);
            this.configurations = configurations;
            this.delay = configurations.getLong(TASK_SCHEDULE_DELAY);
        }

        /**
         * <p>
         * Returns the delay before the next retry task execution.
         * </p>
         *
         * @return delay in milliseconds
         */
        @Override
        public long getNextInterval() {
            return delay;
        }
    }
}