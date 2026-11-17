package com.tlc.crm.core.features.timeline.store;

import com.tlc.crm.core.features.timeline.manager.ConversableTimelineManager;
import com.tlc.crm.entity.timeline.model.Timeline;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link ConversableTimelineManagerStore} store for managing {@link ConversableTimelineManager} instances by {@link Timeline.Category}
 *
 * @author kishore J
 * @version 1.0
 */
public final class ConversableTimelineManagerStore {

    private final Map<Timeline.Category, ConversableTimelineManager> conversableTimelineManagerMap;

    private ConversableTimelineManagerStore() {
        conversableTimelineManagerMap = new HashMap<>();
    }

    private static class InstanceHolder {
        private static final ConversableTimelineManagerStore CONVERSABLE_TIMELINE_MANAGER_STORE = new ConversableTimelineManagerStore();
    }

    /**
     * <p>
     * Gets the {@link ConversableTimelineManagerStore}
     * </p>
     *
     * @return The {@link ConversableTimelineManagerStore}
     */
    public static ConversableTimelineManagerStore getInstance() {
        return InstanceHolder.CONVERSABLE_TIMELINE_MANAGER_STORE;
    }

    /**
     * <p>
     * Adds a {@link ConversableTimelineManager} for the given {@link Timeline.Category}.
     * </p>
     *
     * @param category                Represents the {@link Timeline.Category}
     * @param categoryTimelineManager The manager implementation to register
     */
    public void add(final Timeline.Category category, final ConversableTimelineManager categoryTimelineManager) {
        conversableTimelineManagerMap.put(category, categoryTimelineManager);
    }

    /**
     * <p>
     * Fetches the {@link ConversableTimelineManager} registered for the given {@link Timeline.Category}.
     * </p>
     *
     * @param category Represents the {@link Timeline.Category}
     * @return An {@link Optional} containing the manager if present, otherwise empty
     */
    public Optional<ConversableTimelineManager> get(final Timeline.Category category) {
        return Optional.of(conversableTimelineManagerMap.get(category));
    }

    /**
     * <p>
     * Clears all registered {@link ConversableTimelineManager}
     * </p>
     */
    public void clear() {
        conversableTimelineManagerMap.clear();
    }
}
