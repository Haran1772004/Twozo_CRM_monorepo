package com.tlc.crm.workflow.cache.resolver.internal;

import com.tlc.crm.workflow.api.manager.WorkflowActionManager;
import com.tlc.crm.workflow.api.model.TriggerAction;
import com.tlc.crm.workflow.cache.TriggerActionCache;
import com.tlc.crm.workflow.cache.resolver.TriggerActionResolver;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Provides the implementation for the services offered in {@link TriggerActionResolver}
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see TriggerActionResolver
 */
public final class TriggerActionResolverImpl implements TriggerActionResolver {

    private final TriggerActionCache triggerActionCache;
    private final WorkflowActionManager workFlowActionManager;
    private final ReentrantReadWriteLock reentrantReadWriteLock;

    private TriggerActionResolverImpl() {
        this.triggerActionCache = TriggerActionCache.getInstance();
        this.workFlowActionManager = WorkflowActionManager.getInstance();
        this.reentrantReadWriteLock = new ReentrantReadWriteLock();
    }

    private static class Instance {
        private static final TriggerActionResolverImpl INSTANCE = new TriggerActionResolverImpl();
    }

    /**
     * Gets the instance of {@link TriggerActionResolverImpl}.
     *
     * @return The {@link TriggerActionResolverImpl}.
     */
    public static TriggerActionResolverImpl getInstance() {
        return TriggerActionResolverImpl.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerSourceId Represents the trigger action source id.
     * @return {@link TriggerAction} which contains the trigger action source info.
     */
    @Override
    public TriggerAction get(final Short triggerSourceId) {
        final ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
        final TriggerAction cacheTriggerAction;

        readLock.lock();

        try {
            cacheTriggerAction = triggerActionCache.get(triggerSourceId);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(cacheTriggerAction)) {
            final ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

            writeLock.lock();

            try {
                final TriggerAction triggerAction = workFlowActionManager.getTriggerAction(triggerSourceId);

                if (Objects.nonNull(triggerAction)) {
                    triggerActionCache.setTriggerAction(triggerAction);
                }

                return triggerAction;
            } finally {
                writeLock.unlock();
            }
        }

        return cacheTriggerAction;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Collection} of {@link TriggerAction}.
     */
    @Override
    public Collection<TriggerAction> getAll() {
        final ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
        final Collection<TriggerAction> cacheTriggerActions;

        readLock.lock();

        try {
            cacheTriggerActions = triggerActionCache.getAll();
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(cacheTriggerActions)) {
            final ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

            writeLock.lock();

            try {
                final Collection<TriggerAction> triggerActions = workFlowActionManager.getAllTriggerAction();

                if (Objects.nonNull(triggerActions)) {
                    triggerActionCache.setTriggerActions(triggerActions);
                }

                return triggerActions;
            } finally {
                writeLock.unlock();
            }
        }

        return cacheTriggerActions;
    }

}
