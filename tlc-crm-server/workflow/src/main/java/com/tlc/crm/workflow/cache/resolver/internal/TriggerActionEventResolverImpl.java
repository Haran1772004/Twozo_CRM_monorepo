package com.tlc.crm.workflow.cache.resolver.internal;

import com.tlc.crm.workflow.api.manager.WorkflowActionManager;
import com.tlc.crm.workflow.api.model.TriggerActionEvent;
import com.tlc.crm.workflow.cache.TriggerActionEventCache;
import com.tlc.crm.workflow.cache.resolver.TriggerActionEventResolver;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Provides the implementation for the services offered in {@link TriggerActionEventResolver}
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see TriggerActionEventResolver
 */
public final class TriggerActionEventResolverImpl implements TriggerActionEventResolver {

    private final TriggerActionEventCache triggerActionEventCache;
    private final WorkflowActionManager workFlowActionManager;
    private final ReentrantReadWriteLock reentrantReadWriteLock;

    private TriggerActionEventResolverImpl() {
        this.triggerActionEventCache = TriggerActionEventCache.getInstance();
        this.workFlowActionManager = WorkflowActionManager.getInstance();
        this.reentrantReadWriteLock = new ReentrantReadWriteLock();
    }

    private static class Instance {
        private static final TriggerActionEventResolverImpl INSTANCE = new TriggerActionEventResolverImpl();
    }

    /**
     * Gets the instance of {@link TriggerActionEventResolverImpl}.
     *
     * @return The {@link TriggerActionEventResolverImpl}.
     */
    public static TriggerActionEventResolverImpl getInstance() {
        return TriggerActionEventResolverImpl.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionEventId Represents the trigger action event id.
     * @return {@link TriggerActionEvent} which contains the trigger action event info.
     */
    @Override
    public TriggerActionEvent get(final Long triggerActionEventId) {
        final ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
        final TriggerActionEvent cacheTriggerActionEvent;

        readLock.lock();

        try {
            cacheTriggerActionEvent = triggerActionEventCache.get(triggerActionEventId);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(cacheTriggerActionEvent)) {
            final ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

            writeLock.lock();

            try {
                final TriggerActionEvent triggerActionEvent = workFlowActionManager.getTriggerActionEvent(triggerActionEventId);

                if (Objects.nonNull(triggerActionEvent)) {
                    triggerActionEventCache.setActionEvent(triggerActionEvent);
                }

                return triggerActionEvent;
            } finally {
                writeLock.unlock();
            }
        }

        return cacheTriggerActionEvent;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Collection} of {@link TriggerActionEvent}.
     */
    @Override
    public Collection<TriggerActionEvent> getAll() {
        final ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
        final Collection<TriggerActionEvent> cacheTriggerActionEvents;

        readLock.lock();

        try {
            cacheTriggerActionEvents = triggerActionEventCache.getAll();
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(cacheTriggerActionEvents)) {
            final ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

            writeLock.lock();

            try {
                final Collection<TriggerActionEvent> triggerActionEvents = workFlowActionManager.getAllTriggerActionEvents();

                if (Objects.nonNull(triggerActionEvents)) {
                    triggerActionEventCache.setActionEvents(triggerActionEvents);
                }

                return triggerActionEvents;
            } finally {
                writeLock.unlock();
            }
        }

        return cacheTriggerActionEvents;
    }

}
