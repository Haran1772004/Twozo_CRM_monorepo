package com.tlc.exchange.rate.internal.schedule;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.exchange.rate.service.provider.ExchangeRateServiceProvider;
import com.tlc.l10n.model.currency.Currency;

import com.tlc.exchange.rate.internal.cache.ExchangeRateCache;
import com.tlc.exchange.rate.internal.manager.ExchangeRateManager;
import com.tlc.exchange.rate.internal.model.ExchangeRateContainer;
import com.tlc.exchange.rate.internal.service.AbstractExchangeRateService;
import com.tlc.exchange.rate.service.ExchangeRateService;

import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Provides the functionalities related to scheduling and managing tasks related to exchange-rate related operations.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class ExchangeRateSyncService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeRateSyncService.class);
    private static final String TASK_SCHEDULER_NAME = "currency-exchange-rate";
    private static final String THREAD_POOL_NAME = "currency-exchange-rate-pool";
    private static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");

    private final SchedulerService schedulerService;

    private ExchangeRateSyncTask exchangeRateSyncTask;
    private Long schedulerId;

    private static class Instance {
        private static final ExchangeRateSyncService INSTANCE = new ExchangeRateSyncService();
    }

    private ExchangeRateSyncService() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Gets the {@link ExchangeRateSyncService} Instance
     * </p>
     *
     * @return {@link ExchangeRateSyncService}
     */
    public static ExchangeRateSyncService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            this.exchangeRateSyncTask = new ExchangeRateSyncTask(new ExchangeRateSyncTaskController());
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, exchangeRateSyncTask);
            LOGGER.info("ExchangeRate: Scheduled exchange rate service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId) && Objects.nonNull(schedulerService)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(exchangeRateSyncTask) && exchangeRateSyncTask.isRunning()) {
                exchangeRateSyncTask.stopTask();
            }
            LOGGER.info("ExchangeRate: Scheduled exchange rate service stopped Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * Controls the scheduling and execution of tasks related to exchange rate operations.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    private static class ExchangeRateSyncTaskController extends TaskController {

        private ExchangeRateSyncTaskController() {
            super(TASK_SCHEDULER_NAME);
        }

        /**
         * <p>
         * Calculates the timestamp for the start of the next day, considering the current UTC time and
         * computes the ZonedDateTime for the current UTC time, adds one day, and sets
         * the time to midnight of the next day (00:01:00).
         * </p>
         *
         * @return The timestamp (epoch seconds) for the start of the next day in UTC.
         */
        @Override
        public long getNextInterval() {
            final ZonedDateTime nextInterval = ZonedDateTime.now(UTC_ZONE_ID).plusDays(1).withHour(0).withMinute(1).withSecond(0).withNano(0);

            return nextInterval.toEpochSecond();
        }
    }

    /**
     * <p>
     * Performs scheduled tasks related to exchange rate handling, such as updating exchange rates.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     * @see Task
     */
    private static class ExchangeRateSyncTask extends Task {

        private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeRateSyncTask.class);

        private final ExchangeRateCache exchangeRateCache;
        private final ExchangeRateManager exchangeRateManager;
        private final ExchangeRateService exchangeRateService;

        private ExchangeRateSyncTask(final TaskController controller) {
            super(controller);
            this.exchangeRateCache = ExchangeRateCache.getInstance();
            this.exchangeRateManager = ExchangeRateManager.get();
            this.exchangeRateService = ExchangeRateServiceProvider.get();
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         *
         * @param taskController represents the controller of the task
         */
        @Override
        public void execute(final TaskController taskController) {
            exchangeRateCache.clear();

            try {

                if (exchangeRateManager.getTotalRecords() == 0) {
                    exchangeRateManager.create(fetchAndMapExchangeRates());
                    LOGGER.info("ExchangeRate: Exchange rates successfully loaded");
                } else {
                    exchangeRateManager.update(fetchAndMapExchangeRates());
                    LOGGER.info("ExchangeRate: Exchange rates successfully updated");
                }
            } catch (Throwable e) {
                LOGGER.error("ExchangeRate: Exchange rates update action failed : {}", e.getMessage());
                LOGGER.debug("Stack", e);
            }
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         *
         * @return true
         */
        @Override
        public boolean stopTask() {
            return true;
        }

        /**
         * <p>
         * Fetches the exchange rate from the provider service and maps it to a collection of {@link ExchangeRateContainer}.
         * And serves as a utility for fetching and transforming exchange rate data.
         * </p>
         *
         * @return A collection of {@link ExchangeRateContainer}s containing the mapped exchange rate data.
         */
        private Collection<ExchangeRateContainer> fetchAndMapExchangeRates() {
            final Collection<Currency> supportedCurrencies = exchangeRateService.getSupportedCurrencies();
            final Collection<ExchangeRateContainer> exchangeRateContainers = new ArrayList<>();
            final AbstractExchangeRateService abstractExchangeRateService = (AbstractExchangeRateService) exchangeRateService;

            for (final Currency currency : supportedCurrencies) {
                exchangeRateContainers.add(abstractExchangeRateService.getOnlineExchangeRateContainer(currency));
            }

            return exchangeRateContainers;
        }
    }
}
