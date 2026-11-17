package com.tlc.commons.service.registry.auto.service;

import com.tlc.commons.model.TlcEnum;

import java.util.Optional;

/**
 * <p>
 * Creates and maintains factories for {@link AutoServiceRegistry} impl classes
 * that holds {@link AutoService} with {@link TlcEnum} as key.
 * </p>
 *
 * @param <K> Child instances of {@link TlcEnum}.
 * @param <V> Child instances of {@link AutoService}.
 * @author Ajay.
 * @version 1.0.
 */
public interface AutoServiceRegistry<K extends TlcEnum, V extends AutoService<?>> {

    /**
     * <p>
     * Puts the instances.
     * </p>
     *
     * @param twozoEnum        {@link TlcEnum}.
     * @param twozoAutoService {@link AutoService}.
     */
    void put(final K twozoEnum, final V twozoAutoService);

    /**
     * <p>
     * Opts {@link Optional<V>}.
     * </p>
     *
     * @param twozoEnum {@link TlcEnum}.
     * @return Opts {@link Optional<V>}.
     */
    Optional<V> opt(final K twozoEnum);
}