package com.tlc.crm.workflow.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.crm.workflow.api.manager.WorkflowActionManager;
import com.tlc.crm.workflow.api.model.TriggerActionEvent;
import com.tlc.crm.workflow.cache.TriggerActionEventCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Stores the {@link TriggerActionEvent} into cache
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see TriggerActionEventCache
 */
public final class TriggerActionEventCacheImpl implements TriggerActionEventCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(TriggerActionEventCacheImpl.class);

    private final Cache<Long, TriggerActionEvent> triggerActionEventCache;
    private final WorkflowActionManager workFlowActionManager;

    private TriggerActionEventCacheImpl() {
        this.triggerActionEventCache = CacheManager.getInstance().createCache();
        this.workFlowActionManager = WorkflowActionManager.getInstance();
    }

    private static class Instance {
        private static final TriggerActionEventCacheImpl INSTANCE = new TriggerActionEventCacheImpl();
    }

    /**
     * Gets the {@link TriggerActionEventCacheImpl} instance.
     *
     * @return {@link TriggerActionEventCacheImpl}.
     */
    public static TriggerActionEventCacheImpl getInstance() {
        return TriggerActionEventCacheImpl.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Collection<TriggerActionEvent> triggerActionEvents = workFlowActionManager.getAllTriggerActionEvents();

        if (!triggerActionEvents.isEmpty()) {
            LOGGER.info("Total number of trigger action event data loaded into cache : {}", triggerActionEvents.size());
            triggerActionEventCache.putAll(groupActionEvents(triggerActionEvents));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionEventId Represents the trigger action event id
     * @return A {@link TriggerActionEvent} containing information about the trigger action event
     */
    @Override
    public TriggerActionEvent get(final Long triggerActionEventId) {
        return triggerActionEventCache.get(triggerActionEventId);
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of {@link TriggerActionEvent}.
     */
    @Override
    public Collection<TriggerActionEvent> getAll() {
        final Collection<TriggerActionEvent> triggerActionEvents = new ArrayList<>();

        triggerActionEventCache.forEach((triggerActionEventId, actionEvent) -> triggerActionEvents.add(new TriggerActionEvent.Builder(triggerActionEventId,
                actionEvent.getAliasName(), actionEvent.getDisplayName()).build()));

        return triggerActionEvents;
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionEvent Represents the {@link TriggerActionEvent}.
     */
    @Override
    public void setActionEvent(final TriggerActionEvent triggerActionEvent) {
        triggerActionEventCache.put(triggerActionEvent.id(), triggerActionEvent);
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionEvents Represents the collection of {@link TriggerActionEvent}.
     */
    @Override
    public void setActionEvents(final Collection<TriggerActionEvent> triggerActionEvents) {
        triggerActionEventCache.putAll(groupActionEvents(triggerActionEvents));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        triggerActionEventCache.clear();
    }

    /**
     * <p>
     * Groups a collection of {@link TriggerActionEvent} entities into a {@link Map} using their ids as keys.
     * </p>
     *
     * @param triggerActionEvents Represents the collection of {@link TriggerActionEvent}.
     * @return A map contains where ids serve as keys and corresponding entities as values
     */
    private Map<Long, TriggerActionEvent> groupActionEvents(final Collection<TriggerActionEvent> triggerActionEvents) {
        return triggerActionEvents.stream().collect(Collectors.toMap(TriggerActionEvent::id, Function.identity()));
    }
}
