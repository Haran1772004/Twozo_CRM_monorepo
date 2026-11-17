package com.tlc.sync.service.oauth.internal.controller;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import com.tlc.sync.service.oauth.internal.RefreshTokenHandlerRegistry;
import com.tlc.sync.service.oauth.internal.handler.TokenRefreshHandler;
import com.tlc.sync.service.oauth.internal.manager.SyncManager;
import com.tlc.sync.service.oauth.model.SyncServiceCategory;
import com.tlc.sync.service.oauth.model.RefreshableToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Provides the token refresh related services
 *
 * @author Maharaja S
 * @version 1.0
 */
public class TokenRefreshService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenRefreshService.class);
    private static final String SCHEDULER_NAME = "token-refresh-scheduler";
    private static final String THREAD_POOL_NAME = "token-refresh-pool";

    private final SchedulerService schedulerService;
    private TokenRefreshTask tokenRefreshTask;
    private Long schedulerId;

    private TokenRefreshService() {
        schedulerService = SchedulerServiceProvider.get();
    }

    private static final class InstanceHolder {
        private static final TokenRefreshService TOKEN_REFRESH_SERVICE = new TokenRefreshService();
    }

    /**
     * <p>
     * Gets the instance of {@link TokenRefreshService}
     * </p>
     *
     * @return the instance of {@link TokenRefreshService}
     */
    public static TokenRefreshService getInstance() {
        return InstanceHolder.TOKEN_REFRESH_SERVICE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the configurations
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            tokenRefreshTask = new TokenRefreshTask(new TokenRefreshController());
            schedulerId = schedulerService.schedule(THREAD_POOL_NAME, tokenRefreshTask);

            LOGGER.info("Sync service: Token refresh service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId) && Objects.nonNull(schedulerService)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(tokenRefreshTask) && tokenRefreshTask.isRunning()) {
                tokenRefreshTask.stopTask();
            }
            LOGGER.info("Sync service: Token refresh service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * A task which refreshes the token
     *
     * @author Maharaja S
     * @version 1.0
     */
    private static final class TokenRefreshTask extends Task {

        private final SyncManager syncManager;
        private final RefreshTokenHandlerRegistry refreshTokenHandlerRegistry;

        private TokenRefreshTask(final TaskController controller) {
            super(controller);
            syncManager = SyncManager.getInstance();
            refreshTokenHandlerRegistry = RefreshTokenHandlerRegistry.getInstance();
        }

        /**
         * {@inheritDoc}
         *
         * @param taskController Represents the {@link TaskController}
         */
        @Override
        public void execute(final TaskController taskController) {
            try {
                final Collection<RefreshableToken> refreshableTokens = syncManager.getRefreshableTokens();

                for (final RefreshableToken refreshableToken : refreshableTokens) {
                    final SyncServiceCategory syncServiceCategory = refreshableToken.getSyncServiceCategory();
                    final TokenRefreshHandler tokenRefreshHandler = refreshTokenHandlerRegistry.get(syncServiceCategory);

                    if (Objects.nonNull(tokenRefreshHandler)) {
                        tokenRefreshHandler.handle(refreshableToken);
                        LOGGER.debug("Sync service: Token refreshed successfully. Category:{}, Token Id: {}", syncServiceCategory, refreshableToken.getId());
                    }
                }
            } catch (Throwable throwable) {
                LOGGER.error("Sync service: Token refresh failed : {}, Stack : ", throwable.getMessage(), throwable);
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
     * <p>
     * A controller which controls the token refresh task
     * </p>
     *
     * @author Maharaja S
     * @version 1.0
     */
    private static final class TokenRefreshController extends TaskController {

        private TokenRefreshController() {
            super(SCHEDULER_NAME);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getNextInterval() {
            return TimeUnit.MINUTES.toMillis(5);
        }
    }
}