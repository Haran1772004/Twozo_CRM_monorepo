package com.tlc.exchange.rate.internal.registry;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.exchange.rate.service.ExchangeRateService;

import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * Representing a registry for {@link ExchangeRateService} service.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class ExchangeRateServiceRegistry implements ServiceRegistry<ExchangeRateService> {

    private final AtomicReference<ExchangeRateService> exchangeRateService;

    private static final class Instance {
        private static final ExchangeRateServiceRegistry EXCHANGE_RATE_SERVICE_REGISTRY = new ExchangeRateServiceRegistry();
    }

    private ExchangeRateServiceRegistry() {
        exchangeRateService = new AtomicReference<>();
    }

    public static ServiceRegistry<ExchangeRateService> getInstance() {
        return Instance.EXCHANGE_RATE_SERVICE_REGISTRY;
    }

    /**
     * <p>
     * Registers a {@link ExchangeRateService} with the registry.
     * </p>
     *
     * @param service The {@link ExchangeRateService} to register.
     */
    @Override
    public void register(final ExchangeRateService service) {
        if (!exchangeRateService.compareAndSet(null, service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
        }
    }

    /**
     * <p>
     * Retrieves the registered {@link ExchangeRateService} instance.
     * </p>
     *
     * @return The {@link ExchangeRateService} instance.
     */
    @Override
    public ExchangeRateService get() {
        return exchangeRateService.get();
    }

    /**
     * <p>
     * Unregisters a {@link ExchangeRateService} from the registry.
     * </p>
     *
     * @return The unregistered {@link ExchangeRateService}.
     */
    @Override
    public ExchangeRateService unregister() {
        return exchangeRateService.getAndSet(null);
    }
}
