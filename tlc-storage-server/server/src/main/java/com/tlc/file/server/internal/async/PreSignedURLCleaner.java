package com.tlc.file.server.internal.async;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.file.server.api.manager.AttachmentManager;

import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A service that is responsible for ensuring that any temporary URLs that
 * have been generated for file access are properly cleaned up after their
 * expiration or use, helping to maintain storage security and integrity.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class PreSignedURLCleaner implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreSignedURLCleaner.class);

    private final SchedulerService schedulerService;

    private PreSignedURLCleanerTask preSignedURLCleanerTask;
    private Long schedulerId;

    private PreSignedURLCleaner() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    private static class InstanceHolder {
        private static final PreSignedURLCleaner INSTANCE = new PreSignedURLCleaner();
    }

    public static PreSignedURLCleaner getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations
     */
    @Override
    public void start(final Configurations configurations) {

        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            this.schedulerId = schedulerService.schedule("presigned-url-cleaner-pool",
                    preSignedURLCleanerTask = new PreSignedURLCleanerTask(new PreSignedURLCleanerController("presigned-url-cleaner")));

            LOGGER.info("File service: Pre-signed URL cleanup service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(preSignedURLCleanerTask) && preSignedURLCleanerTask.isRunning()) {
                schedulerService.removeTask(schedulerId);
                LOGGER.info("File service: Pre-signed URL cleanup service stopped, Scheduler ID : {}", schedulerId);
            }
        }
    }

    /**
     * A task which is responsible to do the url cleanup activity.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class PreSignedURLCleanerTask extends Task {

        private final AttachmentManager attachmentManager;

        private PreSignedURLCleanerTask(final TaskController controller) {
            super(controller);
            this.attachmentManager = AttachmentManager.getInstance();
        }

        /**
         * {@inheritDoc}
         *
         * @param taskController Which controls the trigger task.
         */
        @Override
        public void execute(final TaskController taskController) {
            LOGGER.info("File service: Pre-signed URL cleanup activity started");

            try {
                attachmentManager.deletePreSignedURL();
                LOGGER.info("File service: Pre-signed URL cleanup activity finished");
            } catch (Throwable cause) {
                LOGGER.error("File service: Pre-signed URL cleanup activity failed, Error: {}", cause.getMessage());
                LOGGER.debug("Stack: ", cause);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            return true;
        }
    }

    /**
     * A controller which controls pre-signed url cleaner tasks.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class PreSignedURLCleanerController extends TaskController {

        private PreSignedURLCleanerController(final String scheduleName) {
            super(scheduleName);
        }

        /**
         * {@inheritDoc}
         *
         * @return the next interval time for the task is executed in milliseconds.
         */
        @Override
        public long getNextInterval() {
            return TimeUnit.SECONDS.toMillis(5);
        }
    }
}