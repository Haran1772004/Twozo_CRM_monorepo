package com.tlc.crm.server.starter.initializer;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import com.tlc.components.starter.initializer.Modules;

import com.tlc.crm.security.initializer.SecurityServiceInitializer;
import com.tlc.crm.security.permission.annotation.validator.ActionLimitValidator;
import com.tlc.crm.security.permission.annotation.verifier.Verifier;
import com.tlc.crm.security.registrar.SecurityResourceRegistrar;
import com.tlc.crm.security.registrar.model.SecurityRegistrarData;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

/**
 * Responsible for initiating the initialization process of security services
 *
 * @author sundar
 * @version 1.0
 */
@Component
final class SecurityInitializer extends AbstractModuleInitializer {

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
    public void shutdown() {
        SecurityResourceRegistrar.getInstance().unregisterAll();
        SecurityServiceInitializer.getInstance().stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        SecurityServiceInitializer.getInstance().preInitialize(null);
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public void postInitialize() {
        final SecurityResourceRegistrar securityResourceRegistrar = SecurityResourceRegistrar.getInstance();
        final Consumer<Map<Modules, Collection<Class<?>>>> annotationClassRegistrar = (registeredClasses) -> {

            for (final Map.Entry<Modules, Collection<Class<?>>> registeredClassesByModule : registeredClasses.entrySet()) {
                final SecurityRegistrarData securityRegistrarData = new SecurityRegistrarData();
                final Modules modules = registeredClassesByModule.getKey();

                securityRegistrarData.setModuleId(modules.getId());
                securityRegistrarData.setModuleName(modules.getName());
                securityRegistrarData.setClasses(registeredClassesByModule.getValue());

                securityResourceRegistrar.register(securityRegistrarData);
            }
        };
        annotationClassRegistrar.accept(fetchVerifierRegisteredClasses());
        annotationClassRegistrar.accept(fetchLimitValidatorRegisteredClasses());
        SecurityServiceInitializer.getInstance().postInitialize(null);
        LOGGER.info("Application Initializer: Security module is initialized successfully");
    }

    /**
     * Fetches the permission {@link Verifier} registered {@link Class} files and organizes them into a map ordered by module.
     *
     * @return {@link Map} of modules with its {@link Verifier} registered classes.
     */
    private Map<Modules, Collection<Class<?>>> fetchVerifierRegisteredClasses() {
        try {
            final Collection<Class<?>> classes = classScanner.findClassesWithAnnotation("com.tlc", Verifier.class);

            return segregateClassesByModule(classes);
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA);
        }
    }

    /**
     * Fetches the {@link ActionLimitValidator} registered {@link Class} files and organizes them into a map ordered by module.
     *
     * @return {@link Map} of modules with its {@link ActionLimitValidator} registered classes.
     */
    private Map<Modules, Collection<Class<?>>> fetchLimitValidatorRegisteredClasses() {
        try {
            final Collection<Class<?>> classes = classScanner.findClassesWithAnnotation("com.tlc", ActionLimitValidator.class);

            return segregateClassesByModule(classes);
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA, "Unable to register limitation checker annotation registered classes");
        }
    }
}
