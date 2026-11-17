package com.tlc.validator;

import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.validator.internal.ValidatorServiceImpl;
import com.tlc.validator.internal.registry.ValidatorServiceRegistry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author Abishek
 * @version 1.0
 */
public final class ValidatorAccess implements BundleActivator
{
    private final ServiceRegistry<ValidatorService> validatorServiceRegistry = ValidatorServiceRegistry.getInstance();

    @Override
    public void start(BundleContext bundleContext)
    {
        final ValidatorServiceImpl validatorServiceImpl = new ValidatorServiceImpl();

        validatorServiceImpl.start(null);
        validatorServiceRegistry.register(validatorServiceImpl);
    }

    @Override
    public void stop(BundleContext bundleContext)
    {
        final ValidatorServiceImpl validatorServiceImpl = (ValidatorServiceImpl) validatorServiceRegistry.unregister();

        validatorServiceImpl.stop();
    }
}
