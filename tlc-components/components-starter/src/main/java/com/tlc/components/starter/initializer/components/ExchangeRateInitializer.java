package com.tlc.components.starter.initializer.components;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.commons.util.Configurations;

import com.tlc.components.starter.fields.ComponentsStarterFields;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import com.tlc.exchange.rate.internal.registry.ExchangeRateServiceRegistry;
import com.tlc.exchange.rate.internal.schedule.ExchangeRateSyncService;
import com.tlc.exchange.rate.internal.service.FixerExchangeRateService;
import com.tlc.exchange.rate.service.ExchangeRateService;

import org.springframework.stereotype.Component;

/**
 * Responsible for triggering the initialization process of the exchange-rate services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
public final class ExchangeRateInitializer extends AbstractModuleInitializer {

    private final ServiceRegistry<ExchangeRateService> exchangeRateServiceRegistry;

    private ExchangeRateInitializer() {
        this.exchangeRateServiceRegistry = ExchangeRateServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return isServiceEnabled(ComponentsStarterFields.CURRENCY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ServiceRegistry<? extends Service> getServiceRegistry() {
        return exchangeRateServiceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        if (isServiceEnabled()) {
            final Configurations configurations = getProperties(ComponentsStarterFields.CURRENCY);
            final ExchangeRateService exchangeRateService = new FixerExchangeRateService();

            exchangeRateService.start(configurations);
            exchangeRateServiceRegistry.register(exchangeRateService);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception If an error occurs during the post-initialization process.
     */
    @Override
    public void postInitialize() throws Exception {
        if (isServiceEnabled()) {
            ExchangeRateSyncService.getInstance().start(null);
        }
    }
}
