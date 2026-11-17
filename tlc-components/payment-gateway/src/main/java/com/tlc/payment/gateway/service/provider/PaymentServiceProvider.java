package com.tlc.payment.gateway.service.provider;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.payment.gateway.internal.registry.PaymentServiceRegistry;
import com.tlc.payment.gateway.service.PaymentService;

import java.util.Objects;

/**
 * Provides the services that enable the {@link PaymentService} to execute its functions effectively.
 *
 * @author Sathish
 * @version 1.0
 */
public final class PaymentServiceProvider {

    private static final ServiceRegistry<PaymentService> PAYMENT_SERVICE_REGISTRY;

    static {
        PAYMENT_SERVICE_REGISTRY = PaymentServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link PaymentService}.
     */
    public static PaymentService get() {
        final PaymentService service = PAYMENT_SERVICE_REGISTRY.get();

        if (Objects.isNull(service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service not initialized");
        } else {
            return service;
        }
    }
}
