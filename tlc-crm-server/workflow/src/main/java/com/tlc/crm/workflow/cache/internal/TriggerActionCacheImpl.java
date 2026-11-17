package com.tlc.crm.workflow.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.crm.workflow.api.manager.WorkflowActionManager;
import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.TriggerAction;
import com.tlc.crm.workflow.cache.TriggerActionCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Stores the {@link TriggerAction} into cache
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see TriggerActionCache
 */
public final class TriggerActionCacheImpl implements TriggerActionCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(TriggerActionCacheImpl.class);

    private final Cache<Short, Map<Long, TriggerActionSource>> triggerActionCache;
    private final WorkflowActionManager workFlowActionManager;

    private TriggerActionCacheImpl() {
        this.triggerActionCache = CacheManager.getInstance().createCache();
        this.workFlowActionManager = WorkflowActionManager.getInstance();
    }

    private static class Instance {
        private static final TriggerActionCacheImpl INSTANCE = new TriggerActionCacheImpl();
    }

    /**
     * Gets the {@link TriggerActionCacheImpl} instance.
     *
     * @return {@link TriggerActionCacheImpl}.
     */
    public static TriggerActionCacheImpl getInstance() {
        return TriggerActionCacheImpl.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Collection<TriggerAction> triggerActions = workFlowActionManager.getAllTriggerAction();

        if (!triggerActions.isEmpty()) {
            LOGGER.info("Total number of trigger action data loaded into cache : {}", triggerActions.size());
            triggerActionCache.putAll(groupTriggerActions(triggerActions));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerSourceId Represents the trigger source id.
     * @return A map containing trigger action source ids as keys and corresponding {@link TriggerActionSource} as values.
     */
    @Override
    public Map<Long, TriggerActionSource> getActionSourceByTriggerSource(final Short triggerSourceId) {
        return triggerActionCache.get(triggerSourceId);
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerSourceId Represents the trigger source id
     * @return A {@link TriggerAction} containing information about the trigger action source
     */
    @Override
    public TriggerAction get(final Short triggerSourceId) {
        final Map<Long, TriggerActionSource> actionSourceMap = triggerActionCache.get(triggerSourceId);

        return Objects.isNull(actionSourceMap) ? null : new TriggerAction(triggerSourceId, actionSourceMap.values());
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of {@link TriggerAction}.
     */
    @Override
    public Collection<TriggerAction> getAll() {
        final Collection<TriggerAction> triggerActions = new ArrayList<>();

        triggerActionCache.forEach((triggerSourceId, actionSourceMap) -> triggerActions.add(new TriggerAction(triggerSourceId, actionSourceMap.values())));

        return triggerActions;
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerAction Represents the {@link TriggerAction}.
     */
    public void setTriggerAction(final TriggerAction triggerAction) {
        final Collection<TriggerActionSource> triggerActionSources = triggerAction.triggerActionSources();
        final Map<Long, TriggerActionSource> groupedTriggerActionSources = triggerActionSources.stream()
                .collect(Collectors.toMap(TriggerActionSource::id, triggerActionSource -> triggerActionSource));

        triggerActionCache.put(triggerAction.sourceId(), groupedTriggerActionSources);
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActions Represents the collection of {@link TriggerAction}.
     */
    @Override
    public void setTriggerActions(final Collection<TriggerAction> triggerActions) {
        triggerActionCache.putAll(groupTriggerActions(triggerActions));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        triggerActionCache.clear();
    }

    /**
     * <p>
     * Groups a collection of {@link TriggerAction} objects into a nested map structure based on their identifiers
     * </p>
     *
     * @param triggerActions Represents the collection of {@link TriggerAction}.
     * @return A map where the outer map is keyed by trigger source ids,
     * and the inner map is keyed by trigger action source ids, containing corresponding {@link TriggerActionSource} objects.
     */
    private Map<Short, Map<Long, TriggerActionSource>> groupTriggerActions(final Collection<TriggerAction> triggerActions) {
        return triggerActions.stream().collect(Collectors.toMap(TriggerAction::sourceId, TriggerAction ->
                TriggerAction.triggerActionSources().stream().collect(Collectors.toMap(TriggerActionSource::id, Function.identity(), (v1, v2) -> v1, TreeMap::new))));
    }
}
