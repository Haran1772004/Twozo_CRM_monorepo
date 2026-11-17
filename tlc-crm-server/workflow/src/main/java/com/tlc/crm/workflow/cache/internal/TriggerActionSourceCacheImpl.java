package com.tlc.crm.workflow.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.crm.workflow.api.manager.WorkflowActionManager;
import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.cache.TriggerActionSourceCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Stores the {@link TriggerActionSource} into cache
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see TriggerActionSourceCache
 */
public final class TriggerActionSourceCacheImpl implements TriggerActionSourceCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(TriggerActionSourceCacheImpl.class);
    private final Cache<Long, TriggerActionSource> actionSourceCache;
    private final WorkflowActionManager workFlowActionManager;

    private TriggerActionSourceCacheImpl() {
        this.actionSourceCache = CacheManager.getInstance().createCache();
        this.workFlowActionManager = WorkflowActionManager.getInstance();
    }

    private static class Instance {
        private static final TriggerActionSourceCacheImpl INSTANCE = new TriggerActionSourceCacheImpl();
    }

    /**
     * Gets the {@link TriggerActionSourceCacheImpl} instance.
     *
     * @return {@link TriggerActionSourceCacheImpl}.
     */
    public static TriggerActionSourceCacheImpl getInstance() {
        return TriggerActionSourceCacheImpl.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Collection<TriggerActionSource> triggerActionSources = workFlowActionManager.getAllTriggerActionSources();

        if (!triggerActionSources.isEmpty()) {
            LOGGER.info("Total number of trigger action source data loaded into cache : {}", triggerActionSources.size());
            actionSourceCache.putAll(groupActionSources(triggerActionSources));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionSourceId Represents the trigger action source id
     * @return A {@link TriggerActionSource} containing information about the trigger action source
     */
    @Override
    public TriggerActionSource get(final Long triggerActionSourceId) {
        return actionSourceCache.get(triggerActionSourceId);
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of {@link TriggerActionSource}.
     */
    @Override
    public Collection<TriggerActionSource> getAll() {
        final Collection<TriggerActionSource> triggerActionSources = new ArrayList<>();

        actionSourceCache.forEach((triggerActionSourceId, workFlowActionSource) -> triggerActionSources.add(new TriggerActionSource.Builder(triggerActionSourceId,
                workFlowActionSource.getDisplayName(), workFlowActionSource.getAliasName()).setTriggerActionEvents(workFlowActionSource.getTriggerActionEvents()).build()));

        return triggerActionSources;
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionSource Represents the {@link TriggerActionSource}.
     */
    @Override
    public void setTriggerActionSource(final TriggerActionSource triggerActionSource) {
        actionSourceCache.put(triggerActionSource.id(), triggerActionSource);
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionSources Represents the collection of {@link TriggerActionSource}.
     */
    @Override
    public void setTriggerActionSources(final Collection<TriggerActionSource> triggerActionSources) {
        actionSourceCache.putAll(groupActionSources(triggerActionSources));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        actionSourceCache.clear();
    }

    /**
     * <p>
     * Groups a collection of {@link TriggerActionSource} entities into a {@link Map} using their ids as keys.
     * </p>
     *
     * @param triggerActionSources Represents the collection of {@link TriggerActionSource}.
     * @return A map contains where ids serve as keys and corresponding entities as values
     */
    private Map<Long, TriggerActionSource> groupActionSources(final Collection<TriggerActionSource> triggerActionSources) {
        return triggerActionSources.stream().collect(Collectors.toMap(TriggerActionSource::id, Function.identity()));
    }
}
