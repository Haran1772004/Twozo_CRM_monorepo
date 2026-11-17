package com.tlc.crm.workflow.cache.resolver.internal;

import com.tlc.crm.workflow.api.manager.WorkflowActionManager;
import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.cache.TriggerActionSourceCache;
import com.tlc.crm.workflow.cache.resolver.TriggerActionSourceResolver;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Provides the implementation for the services offered in {@link TriggerActionSourceResolver}
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see TriggerActionSourceResolver
 */
public final class TriggerActionSourceResolverImpl implements TriggerActionSourceResolver {

    private final TriggerActionSourceCache triggerActionSourceCache;
    private final WorkflowActionManager workFlowActionManager;
    private final ReentrantReadWriteLock reentrantReadWriteLock;

    private TriggerActionSourceResolverImpl() {
        this.triggerActionSourceCache = TriggerActionSourceCache.getInstance();
        this.workFlowActionManager = WorkflowActionManager.getInstance();
        this.reentrantReadWriteLock = new ReentrantReadWriteLock();
    }

    private static class Instance {
        private static final TriggerActionSourceResolverImpl INSTANCE = new TriggerActionSourceResolverImpl();
    }

    /**
     * Gets the instance of {@link TriggerActionSourceResolverImpl}.
     *
     * @return The {@link TriggerActionSourceResolverImpl}.
     */
    public static TriggerActionSourceResolverImpl getInstance() {
        return TriggerActionSourceResolverImpl.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionSourceId Represents the trigger action source id.
     * @return {@link TriggerActionSource} which contains the trigger action source info.
     */
    @Override
    public TriggerActionSource get(final Long triggerActionSourceId) {
        final ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
        final TriggerActionSource cacheTriggerActionSource;

        readLock.lock();

        try {
            cacheTriggerActionSource = triggerActionSourceCache.get(triggerActionSourceId);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(cacheTriggerActionSource)) {
            final ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

            writeLock.lock();

            try {
                final TriggerActionSource triggerActionSource = workFlowActionManager.getTriggerActionSource(triggerActionSourceId);

                if (Objects.nonNull(triggerActionSource)) {
                    triggerActionSourceCache.setTriggerActionSource(triggerActionSource);
                }

                return triggerActionSource;
            } finally {
                writeLock.unlock();
            }
        }

        return cacheTriggerActionSource;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Collection} of {@link TriggerActionSource}.
     */
    @Override
    public Collection<TriggerActionSource> getAll() {
        final ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
        final Collection<TriggerActionSource> cachedTriggerActionSources;

        readLock.lock();

        try {
            cachedTriggerActionSources = triggerActionSourceCache.getAll();
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(cachedTriggerActionSources) || cachedTriggerActionSources.isEmpty()) {
            final ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

            writeLock.lock();

            try {
                final Collection<TriggerActionSource> triggerActionSources = workFlowActionManager.getAllTriggerActionSources();

                if (Objects.nonNull(triggerActionSources)) {
                    triggerActionSourceCache.setTriggerActionSources(triggerActionSources);
                }

                return triggerActionSources;
            } finally {
                writeLock.unlock();
            }
        }

        return cachedTriggerActionSources;
    }

}
