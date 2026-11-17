package com.tlc.crm.security.permission.model;

/**
 * Provider retrieves the configured value
 *
 * @param <R> Represents the result of the provider
 * @author sundar
 * @version 1.0
 */
public interface SecureDefaultValueProvider<R> {

    /**
     * Gets the name of the value provider
     *
     * @return the name of the value provider
     */
    String name();

    /**
     * Gets the value
     *
     * @return The value
     */
    R get();
}