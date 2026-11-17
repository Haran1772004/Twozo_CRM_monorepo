package com.tlc.components.starter.initializer.components;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import com.tlc.validator.ValidatorService;
import com.tlc.validator.internal.ValidatorServiceImpl;
import com.tlc.validator.internal.registry.ValidatorServiceRegistry;

import org.springframework.stereotype.Component;

/**
 * Responsible for initiating the initialization process of validator services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
final class ValidatorInitializer extends AbstractModuleInitializer {

    private final ServiceRegistry<ValidatorService> validatorServiceRegistry;

    private ValidatorInitializer() {
        this.validatorServiceRegistry = ValidatorServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ServiceRegistry<? extends Service> getServiceRegistry() {
        return validatorServiceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        final ValidatorServiceImpl validatorServiceImpl = new ValidatorServiceImpl();

        validatorServiceImpl.start(null);
        validatorServiceRegistry.register(validatorServiceImpl);
    }
}
