package com.tlc.crm.core.internal.async;

import com.tlc.commons.executor.task.RunnableTask;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.handler.EventDataHandler;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.manager.ContactSyncManager;
import com.tlc.crm.core.api.contact.model.ContactSyncSettings;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.internal.async.cache.ContactSyncCache;
import com.tlc.crm.core.internal.async.cache.ContactSyncInfoService;
import com.tlc.crm.core.internal.contact.sync.ContactSyncService;
import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.crm.entity.async.SyncInfo;
import com.tlc.notification.model.sync.SyncStatus;
import com.tlc.crm.notification.app.Event;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.crm.common.sync.SyncMode;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.service.QueueService;
import com.tlc.queue.service.provider.QueueServiceProvider;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.tlc.crm.notification.app.event.NotificationEvent.APP_NOTIFICATION_EVENT;


/**
 * <p>
 * Provides the services for executing contact synchronization task
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class ContactSyncHandlerImpl implements ContactSyncHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactSyncHandlerImpl.class);

    private final ContactSyncInfoService contactSyncInfoService;
    private final ContactSyncCache contactSyncCache;
    private final SchedulerService schedulerService;
    private final ContactSyncManager contactSyncManager;
    private final ContactSyncService contactSyncService;
    private final EventDataHandler eventDataHandler;
    private final DateTimeUtil dateTimeUtil;
    private final QueueService queueService;

    public ContactSyncHandlerImpl() {
        this.contactSyncInfoService = ContactSyncInfoService.getInstance();
        this.contactSyncCache = ContactSyncCache.getInstance();
        this.schedulerService = SchedulerServiceProvider.get();
        this.contactSyncManager = ContactSyncManager.getInstance();
        this.contactSyncService = ContactSyncService.getInstance();
        this.queueService = QueueServiceProvider.get();
        this.eventDataHandler = EventDataHandler.getInstance();
        this.dateTimeUtil = DateTimeUtil.getInstance();
    }

    private static class Instance {
        private static final ContactSyncHandlerImpl INSTANCE = new ContactSyncHandlerImpl();
    }

    /**
     * <p>
     * Gets the instance of the {@link ContactSyncHandler}
     * </p>
     *
     * @return {@link ContactSyncHandler}
     */
    public static ContactSyncHandlerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param syncSettings Represents the {@link ContactSyncSettings}
     * @return {@link SyncStatus}
     */
    @Override
    public void processSynchronization(final ContactSyncSettings syncSettings) {

        sync(() -> {
            final long orgId = syncSettings.orgId();
            final long userId = syncSettings.getUserId();
            final Long syncId = syncSettings.id();

            contactSyncInfoService.setSyncStatus(orgId, userId, new SyncInfo(syncId, SyncStatus.IN_PROGRESS,
                    syncSettings.getConfig().getConfigAsJson().toString()));

            try {
                contactSyncManager.create(orgId, List.of(syncSettings));

                if (SyncMode.isEquals(SyncMode.TWO_WAY, syncSettings.getConfig().getSyncWay())) {
                    final JsonObject jsonObject = Json.object();

                    jsonObject.put(ContactFields.ORG_ID, orgId);
                    jsonObject.put(ContactFields.USER_ID, userId);
                    LOGGER.info("Contact push event initiated. Sync id : {}, Org Id : {}", syncId, orgId);
                    final SubscriptionTopic contactPushEventTopic = CoreQueueTopics.getContactPushEventTopic(QueueModel.Type.KAFKA);
                    final KafkaProducerConfig contactPushEventProducerConfig = new KafkaProducerConfig(contactPushEventTopic);

                    try (final Producer<byte[]> contactPushEventProducer = queueService.createProducer(contactPushEventProducerConfig)) {
                        contactPushEventProducer.send(new StreamMessage<>(String.valueOf(orgId), contactPushEventTopic, jsonObject.getBytes()));
                    }
                }
                contactSyncService.loadContacts(syncSettings);
                contactSyncInfoService.setSyncStatus(orgId, userId, new SyncInfo(syncId, dateTimeUtil.getTimeStamp(syncSettings.getLastSyncTime()), SyncStatus.SYNCED, null));
                final EventData eventData = new SecureEventData(orgId, userId);

                eventData.setValues(List.of(syncSettings));
                eventData.setAction(Action.CREATE);
                eventDataHandler.handleEvent(APP_NOTIFICATION_EVENT, eventData, Event.Type.POST);

                LOGGER.debug("Contact sync: Synchronization process has been successfully completed for Org ID: {}, User ID: {}, Sync ID: {}", orgId, userId, syncId);
            } catch (Throwable throwable) {
                LOGGER.error("Contact sync: Failure occurred while attempting synchronization for Org ID: {}, User ID: {}, Sync ID: {}, Stack: {}", orgId, userId, syncId, throwable);
                contactSyncInfoService.setSyncStatus(orgId, userId, new SyncInfo(syncId, SyncStatus.FAILED));
            }
        });
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the synced contact id
     * @return {@link SyncInfo}
     */
    @Override
    public SyncInfo getSyncInfo(final long orgId, final long syncId) {
        return contactSyncInfoService.get(orgId, syncId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the sync id
     * @return {@link SyncStatus}
     */
    @Override
    public SyncStatus getSyncStatus(final long orgId, final long syncId) {
        return getSyncInfo(orgId, syncId).syncStatus();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param syncId Represents the sync id
     */
    @Override
    public void remove(final long syncId) {
        contactSyncCache.remove(syncId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void clear() {
        contactSyncCache.clear();
    }

    /**
     * <p>
     * Asynchronously a task to the executor for processing the contact sync.
     * </p>
     *
     * @param task Represents the {@link RunnableTask}
     */
    private void sync(final RunnableTask task) {
        final ContactSyncTask contactSyncTask = new ContactSyncTask(new ContactSyncTask.ContactSyncTaskController(), task);

        schedulerService.schedule("contact-sync-thread-pool", contactSyncTask);
    }

    /**
     * <p>
     * A task which is responsible to do the contact sync action.
     * </p>
     *
     * @author NandiniRakAs
     * @version 1.0
     * @see Task
     */
    private static final class ContactSyncTask extends Task {

        private final RunnableTask task;

        private ContactSyncTask(final TaskController controller, final RunnableTask task) {
            super(controller, 0);
            this.task = task;
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         *
         * @param taskController Represents the {@link TaskController}
         */
        @Override
        public void execute(final TaskController taskController) {
            task.run();
        }

        /**
         * <p>
         * Provides the specialized controller for managing the execution of contact synchronization task.
         * </p>
         *
         * @author NandiniRakAS
         * @version 1.0
         * @see TaskController
         */
        private static class ContactSyncTaskController extends TaskController {

            private ContactSyncTaskController() {
                super("contact-sync-processor");
            }

            /**
             * <p>
             * {@inheritDoc}
             * </p>
             *
             * @return the next interval
             */
            @Override
            public long getNextInterval() {
                return 0;
            }
        }
    }
}
