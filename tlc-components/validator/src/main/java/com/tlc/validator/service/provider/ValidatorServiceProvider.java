package com.tlc.validator.service.provider;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.validator.ValidatorService;
import com.tlc.validator.internal.registry.ValidatorServiceRegistry;

/**
 * Provides the services that enable the {@link ValidatorService} to execute its functions effectively.
 *
 * @author Sathish
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ValidatorServiceProvider {

    private static final ServiceRegistry<ValidatorService> VALIDATOR_SERVICE_REGISTRY;

    static {
        VALIDATOR_SERVICE_REGISTRY = ValidatorServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link ValidatorService}.
     */
    public static ValidatorService get() {
        final ValidatorService validatorService = VALIDATOR_SERVICE_REGISTRY.get();

        if (validatorService == null) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service not initialized");
        } else {
            return validatorService;
        }
    }
}
