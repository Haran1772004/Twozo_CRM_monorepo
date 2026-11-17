package com.tlc.commons.service.registry.auto.service;

import com.tlc.commons.model.TlcEnum;

/**
 * <p>
 * Facilitates to dynamically register the service class.
 * </p>
 *
 * @param <T> {@link TlcEnum}.
 * @author Ajay.
 * @version 1.0.
 */
public interface AutoService<T extends TlcEnum> {

    /**
     * <p>
     * Gets {@link TlcEnum}.
     * </p>
     *
     * @return {@link TlcEnum}.
     */
    T getType();
}