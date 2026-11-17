package com.tlc.crm.workflow.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.crm.workflow.api.manager.WorkflowActionManager;
import com.tlc.crm.workflow.api.model.TriggerActionField;
import com.tlc.crm.workflow.cache.TriggerActionFieldCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * Stores the {@link TriggerActionField} into cache
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see TriggerActionFieldCache
 */
public final class TriggerActionFieldCacheImpl implements TriggerActionFieldCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(TriggerActionFieldCacheImpl.class);
    private final Cache<Long, Collection<TriggerActionField>> actionFieldCache;
    private final WorkflowActionManager workflowActionManager;

    private TriggerActionFieldCacheImpl() {
        this.actionFieldCache = CacheManager.getInstance().createCache();
        this.workflowActionManager = WorkflowActionManager.getInstance();
    }

    private static class Instance {
        private static final TriggerActionFieldCacheImpl INSTANCE = new TriggerActionFieldCacheImpl();
    }

    /**
     * Gets the {@link TriggerActionFieldCacheImpl} instance.
     *
     * @return {@link TriggerActionFieldCacheImpl}.
     */
    public static TriggerActionFieldCacheImpl getInstance() {
        return TriggerActionFieldCacheImpl.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Collection<TriggerActionField> triggerActionFields = workflowActionManager.getAllTriggerActionFields();

        if (!triggerActionFields.isEmpty()) {
            LOGGER.info("Total number of trigger action field data loaded into cache : {}", triggerActionFields.size());
            actionFieldCache.putAll(groupActionFields(triggerActionFields));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionId Represents the trigger action id
     * @return A map containing trigger action field ids as keys and corresponding {@link TriggerActionField} objects as values
     */
    @Override
    public Collection<TriggerActionField> get(final Long triggerActionId) {
        return actionFieldCache.get(triggerActionId);
    }

    /**
     * {@inheritDoc}
     *
     * @return The map containing trigger action ids as keys and corresponding map of trigger action field ids to {@link TriggerActionField} objects as values
     */
    @Override
    public Map<Long, Collection<TriggerActionField>> getAll() {
        final Map<Long, Collection<TriggerActionField>> actionFieldsMap = new HashMap<>();

        actionFieldCache.forEach(actionFieldsMap::put);

        return actionFieldsMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        actionFieldCache.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionFields Represents the collection of {@link TriggerActionField}.
     */
    @Override
    public void setTriggerActionFields(final Collection<TriggerActionField> triggerActionFields) {
        actionFieldCache.putAll(groupActionFields(triggerActionFields));
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionField Represents the {@link TriggerActionField}.
     */
    @Override
    public void setTriggerActionField(final TriggerActionField triggerActionField) {
        actionFieldCache.putAll(groupActionFields(List.of(triggerActionField)));
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionFields Represents the collection of {@link TriggerActionField}
     * @return A map containing trigger action ids as keys, each associated with a nested map of trigger action field ids to {@link TriggerActionField} objects.
     */
    @Override
    public Map<Long, Collection<TriggerActionField>> groupActionFields(final Collection<TriggerActionField> triggerActionFields) {
        return triggerActionFields.stream().collect(Collectors.groupingBy(TriggerActionField::getTriggerActionId, Collectors.toCollection(ArrayList::new)));
    }
}
