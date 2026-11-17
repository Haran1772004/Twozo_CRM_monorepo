package com.tlc.observability.internal.utils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 *  CommonUtil is a utility class that provides common helper methods used throughout the application.
 * <p>
 * <b>Author:</b> Ashok<br>
 * <b>Version:</b> 1.0<br>
 * <b>Since:</b> 2024
 * </p>
 */
public class CommonUtil {

    /**
     * Utility method to retrieve the object from the atomic reference.
     *
     * @return the instance present in the atomic reference
     */
    public static <T> T getNonNullEntity(AtomicReference<T> atomicRef, final String errorMessage) {
        final T entity = atomicRef.get();

        if (Objects.isNull(entity)) {
            throw new IllegalStateException("Entity is not present in the AtomicReference.");
        }

        return entity;
    }

}
