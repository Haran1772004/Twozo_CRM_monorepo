package com.tlc.exchange.rate;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.service.TlcActivator;
import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

import com.tlc.exchange.rate.internal.schedule.ExchangeRateSyncService;
import com.tlc.exchange.rate.internal.service.FixerExchangeRateService;
import com.tlc.exchange.rate.internal.registry.ExchangeRateServiceRegistry;
import com.tlc.exchange.rate.internal.status.ExchangeRateErrorCodes;
import com.tlc.exchange.rate.service.ExchangeRateService;

import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Activates the exchange-rate related functionalities and encapsulates the logic involved in managing exchange rates.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class ExchangeRateAccess extends TlcActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeRateAccess.class);

    private final ServiceRegistry<ExchangeRateService> exchangeRateServiceRegistry;
    private final ExchangeRateSyncService exchangeRateSyncService;

    public ExchangeRateAccess() {
        exchangeRateServiceRegistry = ExchangeRateServiceRegistry.getInstance();
        exchangeRateSyncService = ExchangeRateSyncService.getInstance();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param bundleContext The execution context of the bundle being started.
     * @throws Exception
     */
    @Override
    public void bundleStart(final BundleContext bundleContext) {
        final Configurations configurations = ConfHolder.getInstance().getProperties("currency");
        final ExchangeRateService exchangeRateService = new FixerExchangeRateService();

        try {
            exchangeRateService.start(configurations);
            exchangeRateServiceRegistry.register(exchangeRateService);
            LOGGER.info("ExchangeRate: Exchange rate service initialized successfully");
        } catch (Throwable e) {
            LOGGER.error("ExchangeRate: Exchange rate service initialization failed");
            throw ErrorCode.get(ExchangeRateErrorCodes.EXCHANGE_RATE_SERVICE_NOT_INITIALIZED);
        }

        LOGGER.info("ExchangeRate: Exchange rate module started");
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void bundleStop(final BundleContext bundleContext) {
        final ExchangeRateService exchangeRateService = exchangeRateServiceRegistry.unregister();

        exchangeRateService.stop();
        exchangeRateSyncService.stop();
        LOGGER.info("ExchangeRate: Exchange rate module stopped");
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void preRequisiteBundlesLoaded(final BundleContext bundleContext) {
        LOGGER.info("Prerequisite startup activities done {}", bundleContext.getBundle().getSymbolicName());
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void allBundlesLoaded(final BundleContext bundleContext) {
        exchangeRateSyncService.start(null);
        LOGGER.info("Processes done after all bundles are loaded for {}", bundleContext.getBundle().getSymbolicName());
    }
}