package com.tlc.crm.workflow.cache.resolver.internal;

import com.tlc.crm.workflow.api.manager.WorkflowActionManager;
import com.tlc.crm.workflow.api.model.TriggerActionField;
import com.tlc.crm.workflow.cache.TriggerActionFieldCache;
import com.tlc.crm.workflow.cache.resolver.TriggerActionEventResolver;
import com.tlc.crm.workflow.cache.resolver.TriggerActionFieldResolver;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Provides the implementation for the services offered in {@link TriggerActionEventResolver}
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see TriggerActionFieldResolver
 */
public final class TriggerActionFieldResolverImpl implements TriggerActionFieldResolver {

    private final TriggerActionFieldCache triggerActionFieldCache;
    private final WorkflowActionManager workFlowActionManager;
    private final ReentrantReadWriteLock lock;

    private static final class Instance {
        private static final TriggerActionFieldResolverImpl INSTANCE = new TriggerActionFieldResolverImpl();
    }

    private TriggerActionFieldResolverImpl() {
        this.workFlowActionManager = WorkflowActionManager.getInstance();
        this.triggerActionFieldCache = TriggerActionFieldCache.getInstance();
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * Gets the {@link TriggerActionFieldResolverImpl} instance
     *
     * @return {@link TriggerActionFieldResolverImpl}
     */
    public static TriggerActionFieldResolver getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionId Represents the trigger action id
     * @return {@link Map} that containing the trigger action field id as keys and {@link TriggerActionField} as values
     */
    @Override
    public Collection<TriggerActionField> getTriggerActionFields(final Long triggerActionId) {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final Collection<TriggerActionField> cacheTriggerActionFields;

        readLock.lock();

        try {
            cacheTriggerActionFields = triggerActionFieldCache.get(triggerActionId);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(cacheTriggerActionFields) || cacheTriggerActionFields.isEmpty()) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final Collection<TriggerActionField> triggerActionFields = workFlowActionManager.getTriggerActionField(triggerActionId);

                if (Objects.nonNull(triggerActionFields)) {
                    triggerActionFieldCache.setTriggerActionFields(triggerActionFields);
                }

                return triggerActionFields;
            } finally {
                writeLock.unlock();
            }
        }

        return cacheTriggerActionFields;
    }

    /**
     * {@inheritDoc}
     *
     * @return The map containing trigger action ids as keys and corresponding map of trigger action field ids to {@link TriggerActionField} objects as values
     */
    @Override
    public Map<Long, Collection<TriggerActionField>> getAll() {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final Map<Long, Collection<TriggerActionField>> cachedActionFields;

        readLock.lock();

        try {
            cachedActionFields = triggerActionFieldCache.getAll();
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(cachedActionFields) || cachedActionFields.isEmpty()) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final Collection<TriggerActionField> triggerActionFields = workFlowActionManager.getAllTriggerActionFields();

                if (Objects.isNull(triggerActionFields)) {
                    return null;
                }
                triggerActionFieldCache.setTriggerActionFields(triggerActionFields);

                return triggerActionFieldCache.groupActionFields(triggerActionFields);
            } finally {
                writeLock.unlock();
            }
        }

        return cachedActionFields;
    }

}
