package com.tlc.iam.server.internal.session;

import com.tlc.cluster.DistributedLock;
import com.tlc.cluster.service.ClusterService;
import com.tlc.cluster.service.provider.ClusterServiceProvider;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;

/**
 * A service that is responsible to trigger and control the session cleanup actions
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class SessionCleaner implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionCleaner.class);
    private static final String THREAD_POOL_NAME = "session-cleaner-pool";
    private static final String TASK_SCHEDULER_NAME = "session-cleaner";

    private Long schedulerId;

    private static class Instance {
        private static final SessionCleaner INSTANCE = new SessionCleaner();
    }

    private SessionCleaner() {
    }

    public static SessionCleaner getInstance() {
        return Instance.INSTANCE;
    }

    @Override
    public void start(final Configurations configurations) {
        final SchedulerService schedulerService = SchedulerServiceProvider.get();

        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, new SessionCleanerTask(new SessionCleanerController()));
            LOGGER.info("Session cleaner service started, Scheduler ID : {}", schedulerId);
        }

    }

    @Override
    public void stop() {

        if (Objects.nonNull(schedulerId)) {
            final SchedulerService schedulerService = SchedulerServiceProvider.get();

            if (Objects.nonNull(schedulerService)) {
                schedulerService.removeTask(schedulerId);
                LOGGER.info("Session cleaner service stopped, Scheduler ID : {}", schedulerId);
            }
        }

    }

    /**
     * A task which is responsible to do the session cleanup activity
     *
     * @author Selvakumar
     * @version 1.0
     */
    private static class SessionCleanerTask extends Task {

        private final ClusterService clusterService;

        private SessionCleanerTask(final TaskController controller) {
            super(controller);
            clusterService = ClusterServiceProvider.get();
        }

        @Override
        public void execute(final TaskController taskController) {
            try {
                DistributedLock distributedLock = null;

                try {
                    distributedLock = clusterService.getDistributedLock("session-cleaner");
                    distributedLock.acquire();
                    cleanupExpiredSessions();
                } finally {
                    if (Objects.nonNull(distributedLock)) {
                        distributedLock.release();
                    }
                }
            } catch (Throwable e) {
                LOGGER.info("Session cleanup activity failed, Error: {}", e.getMessage());
                LOGGER.debug("Stack", e);
            }
        }

        /**
         * Deletes the expired sessions
         */
        private void cleanupExpiredSessions() {
            final SessionManager sessionManager = SessionManager.getInstance();
            final int batchSize = 1000;

            LOGGER.info("Session cleanup activity started");

            while (true) {
                final long expiredSessionsCount = sessionManager.fetchExpiredSessionsCount();

                LOGGER.info("Number of expired sessions identified : {}", expiredSessionsCount);

                if (expiredSessionsCount > 0) {
                    //TODO no limit clause on delete query
                    final Collection<Long> sessionIds = sessionManager.fetchExpiredSessionIds(batchSize);

                    LOGGER.info("Deleting expired sessions : {}", sessionIds.size());
                    sessionManager.deleteById(sessionIds);

                    if (sessionIds.size() < batchSize) {
                        break;
                    }
                } else {
                    break;
                }

            }

            LOGGER.info("Session cleanup activity completed");
        }

        @Override
        public boolean stopTask() {
            return true;
        }
    }

    /**
     * A controller which controls the session cleaner task {@link SessionCleanerTask}.
     *
     * @author Selvakumar
     * @version 1.0
     */
    private static class SessionCleanerController extends TaskController {

        private SessionCleanerController() {
            super(TASK_SCHEDULER_NAME);
        }

        @Override
        public long getNextInterval() {
            return 600000; //10 minutes
        }
    }

}

