package com.tlc.crm.core.action.contact.scheduler;

import com.tlc.cluster.DistributedLock;
import com.tlc.cluster.service.ClusterService;
import com.tlc.cluster.service.provider.ClusterServiceProvider;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.core.internal.contact.sync.ContactSyncService;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * A service that is responsible to trigger and control the contact syncing actions
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class ContactPullController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactPullController.class);
    private static final String THREAD_POOL_NAME = "contact-pull-pool";
    private static final String TASK_SCHEDULER_NAME = "contact-pull";
    private final SchedulerService schedulerService;
    private Long syncSchedulerId;
    private ContactSyncTask contactSyncTask;

    private static class Instance {
        private static final ContactPullController INSTANCE = new ContactPullController();
    }

    private ContactPullController() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Gets the {@link ContactPullController} Instance
     * </p>
     *
     * @return {@link ContactPullController}
     */
    public static ContactPullController getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the given input
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(syncSchedulerId) && Objects.nonNull(schedulerService)) {
            this.contactSyncTask = new ContactSyncTask(new ContactSyncTaskController());
            this.syncSchedulerId = schedulerService.schedule(THREAD_POOL_NAME, contactSyncTask);

            LOGGER.debug("Contact sync service started, Scheduler ID : {}", syncSchedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(syncSchedulerId) && Objects.nonNull(schedulerService)) {
            schedulerService.removeTask(syncSchedulerId);

            if (Objects.nonNull(contactSyncTask) && contactSyncTask.isRunning()) {
                contactSyncTask.stopTask();
            }

            LOGGER.debug("Contact sync service stopped, Scheduler ID : {}", syncSchedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible for contact syncing activity
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class ContactSyncTask extends Task {

        private final ContactSyncService contactSyncService;
        private final ClusterService clusterService;

        private ContactSyncTask(final TaskController controller) {
            super(controller);
            contactSyncService = ContactSyncService.getInstance();
            clusterService = ClusterServiceProvider.get();
        }

        /**
         * <p>
         * Executes the task which is scheduled
         * </p>
         *
         * @param taskController represents the controller of the task
         */
        @Override
        public void execute(final TaskController taskController) { //Todo need to change the logic
            try {
                DistributedLock distributedLock = null;

                try {
                    distributedLock = clusterService.getDistributedLock("pull-contacts");
                    distributedLock.acquire();
                    contactSyncService.pullContactsFromProvider();
                } finally {
                    if (Objects.nonNull(distributedLock)) {
                        distributedLock.release();
                    }
                }
            } catch (Throwable e) {
                LOGGER.debug("Contact syncing action failed : {}", e.getMessage());
                LOGGER.debug("Stack", e);
            }
        }

        /**
         * {@inheritDoc}
         *
         * @return true
         */
        @Override
        public boolean stopTask() {
            return true;
        }
    }

    /**
     * A controller which controls contact syncing task {@link ContactSyncTask}.
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    public static class ContactSyncTaskController extends TaskController {

        public ContactSyncTaskController() {
            super(TASK_SCHEDULER_NAME);
        }

        /**
         * {@inheritDoc}
         *
         * @return long of interval time
         */
        @Override
        public long getNextInterval() {
            return TimeUnit.SECONDS.toMillis(5);
        }
    }
}