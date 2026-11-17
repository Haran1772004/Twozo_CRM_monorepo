package com.tlc.commons.service.registry.auto.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Annotates the {@link AutoService} impl classes with {@link AutoServiceRegistry} type class for dynamically
 * registering instances to the registry.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoServiceRegistrar {

    /**
     * <p>
     * Impl class of the registry.
     * </p>
     *
     * @return impl class of the registry.
     */
    Class<? extends AutoServiceRegistry<?, ?>> registryClazz();
}
