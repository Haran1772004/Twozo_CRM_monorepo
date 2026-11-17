package com.tlc.crm.entity.resolver;

import com.tlc.commons.concurrent.LockStore;
import com.tlc.crm.entity.cache.OptionCache;
import com.tlc.crm.entity.manager.field.type.OptionManager;
import com.tlc.crm.entity.models.type.Option;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Resolves the {@link Option} related details
 *
 * @author Kishore J
 * @version 1.0
 * @see OptionResolver
 */
public class OptionResolverImpl implements OptionResolver {

    private static final byte STRIPES_COUNT = 10;

    private final OptionCache optionCache;
    private final OptionManager optionManager;
    private final LockStore<Long> lockStore;

    private OptionResolverImpl() {
        optionCache = OptionCache.getInstance();
        optionManager = OptionManager.getInstance();
        lockStore = new LockStore<>(STRIPES_COUNT);
    }

    private static class Instance {
        private static final OptionResolverImpl INSTANCE = new OptionResolverImpl();
    }

    /**
     * <p>
     * Gets the {@link OptionResolverImpl} instance
     * </p>
     *
     * @return The {@link OptionResolverImpl}
     */
    public static OptionResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param fieldId Represents the field id
     * @return The collection of {@link Option}
     */
    @Override
    public Collection<Option> get(final long orgId, final long fieldId) {
        final ReentrantReadWriteLock reentrantReadWriteLock = lockStore.getLock(orgId);
        final ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
        final Collection<Option> existingOptions;

        readLock.lock();

        try {
            existingOptions = optionCache.getOptions(orgId, fieldId);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(existingOptions) || existingOptions.isEmpty()) {
            final ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

            writeLock.lock();

            try {
                final Collection<Option> options = optionManager.getOptions(orgId, fieldId);

                if (Objects.nonNull(options) && !options.isEmpty()) {
                    optionCache.setOptions(orgId, fieldId, options);

                    return options;
                }
            } finally {
                writeLock.unlock();
            }
        }

        return existingOptions;
    }
}
