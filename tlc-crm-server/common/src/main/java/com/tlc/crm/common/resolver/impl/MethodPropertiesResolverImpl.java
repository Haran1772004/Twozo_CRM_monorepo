package com.tlc.crm.common.resolver.impl;

import com.tlc.commons.concurrent.LockStore;
import com.tlc.crm.common.cache.MethodPropertiesCache;
import com.tlc.crm.common.resolver.MethodPropertiesResolver;
import com.tlc.validator.TlcModel;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Resolves the meta data of the {@link Method}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public class MethodPropertiesResolverImpl implements MethodPropertiesResolver {

    private static final byte STRIPES_COUNT = 10;

    private final MethodPropertiesCache methodPropertiesCache;
    private final LockStore<Class<? extends TlcModel>> lockStore;

    private static class Instance {
        private static final MethodPropertiesResolverImpl INSTANCE = new MethodPropertiesResolverImpl();
    }

    public static MethodPropertiesResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    private MethodPropertiesResolverImpl() {
        methodPropertiesCache = MethodPropertiesCache.getInstance();
        lockStore = new LockStore<>(STRIPES_COUNT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Method> getMethods(final Class<? extends TlcModel> modelClass) {
        final ReentrantReadWriteLock reentrantReadWriteLock = lockStore.getLock(modelClass);
        final ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
        final Collection<Method> existingMethods;

        readLock.lock();

        try {
            existingMethods = methodPropertiesCache.getMethods(modelClass);
        } finally {
            readLock.unlock();
        }

        if (existingMethods.isEmpty()) {
            final ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

            writeLock.lock();
            final Collection<Method> methods = new HashSet<>();

            try {
                Class<?> className = modelClass;

                while (Objects.nonNull(className)) {
                    methods.addAll(Arrays.asList(className.getDeclaredMethods()));
                    className = className.getSuperclass();
                }
                methodPropertiesCache.add(modelClass, methods);
            } finally {
                writeLock.unlock();
            }
            return methods;
        }

        return existingMethods;
    }
}
