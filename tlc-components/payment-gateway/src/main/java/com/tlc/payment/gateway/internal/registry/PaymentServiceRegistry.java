package com.tlc.payment.gateway.internal.registry;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.payment.gateway.service.PaymentService;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Registers the {@link PaymentService} into the registry.
 *
 * @author Sathish
 * @version 1.0
 */
public final class PaymentServiceRegistry implements ServiceRegistry<PaymentService> {

    private final AtomicReference<PaymentService> paymentService;

    private static final class Instance {
        private static final PaymentServiceRegistry SEARCH_SERVICE_REGISTRY = new PaymentServiceRegistry();
    }

    private PaymentServiceRegistry() {
        this.paymentService = new AtomicReference<>();
    }

    public static ServiceRegistry<PaymentService> getInstance() {
        return Instance.SEARCH_SERVICE_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @param service The service to register.
     */
    @Override
    public void register(final PaymentService service) {
        if (!paymentService.compareAndSet(null, service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public PaymentService get() {
        return paymentService.get();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link PaymentService}.
     */
    @Override
    public PaymentService unregister() {
        return paymentService.getAndSet(null);
    }
}
