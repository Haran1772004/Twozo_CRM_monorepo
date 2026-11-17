package com.tlc.validator.internal.registry;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.validator.ValidatorService;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Registers the {@link ValidatorService} into the registry.
 *
 * @author Sathish
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ValidatorServiceRegistry implements ServiceRegistry<ValidatorService> {

    private final AtomicReference<ValidatorService> validatorService;

    private static final class Instance {
        private static final ValidatorServiceRegistry VALIDATOR_SERVICE_REGISTRY = new ValidatorServiceRegistry();
    }

    private ValidatorServiceRegistry() {
        this.validatorService = new AtomicReference<>();
    }

    public static ServiceRegistry<ValidatorService> getInstance() {
        return Instance.VALIDATOR_SERVICE_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @param service The service to register.
     */
    @Override
    public void register(final ValidatorService service) {
        if (!validatorService.compareAndSet(null, service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public ValidatorService get() {
        return validatorService.get();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link ValidatorService}.
     */
    @Override
    public ValidatorService unregister() {
        return validatorService.getAndSet(null);
    }
}
