package com.tlc.exchange.rate.service.provider;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.exchange.rate.internal.registry.ExchangeRateServiceRegistry;
import com.tlc.exchange.rate.service.ExchangeRateService;

import java.util.Objects;

/**
 * <p>
 * Provides the instance of the {@link ExchangeRateService}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class ExchangeRateServiceProvider {

    private static final ServiceRegistry<ExchangeRateService> EXCHANGE_RATE_SERVICE_REGISTRY;

    static {
        EXCHANGE_RATE_SERVICE_REGISTRY = ExchangeRateServiceRegistry.getInstance();
    }

    /**
     * <p>
     * Retrieves the registered {@link ExchangeRateService} instance.
     * </p>
     *
     * @return The {@link ExchangeRateService} instance.
     */
    public static ExchangeRateService get() {
        final ExchangeRateService service = EXCHANGE_RATE_SERVICE_REGISTRY.get();

        if (Objects.isNull(service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service not initialized");
        } else {
            return service;
        }
    }
}
