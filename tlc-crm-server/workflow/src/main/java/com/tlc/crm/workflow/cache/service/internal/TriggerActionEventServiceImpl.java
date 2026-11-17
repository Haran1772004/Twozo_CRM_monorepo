package com.tlc.crm.workflow.cache.service.internal;

import com.tlc.crm.workflow.api.model.TriggerActionEvent;

import com.tlc.crm.workflow.cache.resolver.TriggerActionEventResolver;
import com.tlc.crm.workflow.cache.service.TriggerActionEventService;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the implementation for the services offered by {@link TriggerActionEvent}
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see TriggerActionEventService
 */
public class TriggerActionEventServiceImpl implements TriggerActionEventService {

    private final TriggerActionEventResolver triggerActionEventResolver;

    private TriggerActionEventServiceImpl() {
        this.triggerActionEventResolver = TriggerActionEventResolver.getInstance();
    }

    private static class Instance {
        private static final TriggerActionEventServiceImpl INSTANCE = new TriggerActionEventServiceImpl();
    }

    /**
     * Gets the instance of {@link TriggerActionEventServiceImpl}.
     *
     * @return The {@link TriggerActionEventServiceImpl}.
     */
    public static TriggerActionEventServiceImpl getInstance() {
        return TriggerActionEventServiceImpl.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionEventId Represents the trigger action event id.
     * @return The {@link TriggerActionEvent}.
     */
    @Override
    public TriggerActionEvent get(final Long triggerActionEventId) {
        return triggerActionEventResolver.get(triggerActionEventId);
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of {@link TriggerActionEvent}.
     */
    @Override
    public Collection<TriggerActionEvent> getAll() {
        return triggerActionEventResolver.getAll();
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionEventId Represents the trigger action event id.
     * @return If the trigger action event exists it will return true, otherwise it will return false.
     */
    @Override
    public boolean exists(final Long triggerActionEventId) {
        return Objects.nonNull(get(triggerActionEventId));
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionEventIds Which contains the collection of trigger action event ids.
     * @return If all the trigger action event exists it will return true, otherwise it will return false.
     */
    @Override
    public boolean exists(final Collection<Long> triggerActionEventIds) {
        if (triggerActionEventIds.isEmpty()) {
            return false;
        } else {
            final Collection<TriggerActionEvent> triggerActionEvents = getAll();

            return Objects.nonNull(triggerActionEvents) && triggerActionEvents.stream().map(TriggerActionEvent::id)
                    .collect(Collectors.toUnmodifiableSet()).containsAll(triggerActionEventIds);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionEventIds The collection of trigger action event ids
     * @param aliasName             The alias name to compare
     * @return {@code true} if the alias name matches any of the trigger action event ids, otherwise {@code false}
     */
    @Override
    public boolean hasMatchingAliasName(final Collection<Long> triggerActionEventIds, final String aliasName) {
        if (triggerActionEventIds.isEmpty()) {
            return false;
        } else {
            final Collection<TriggerActionEvent> triggerActionEvents = getAll();

            return Objects.nonNull(triggerActionEvents) && triggerActionEvents.stream().anyMatch(actionEvent ->
                    triggerActionEventIds.contains(actionEvent.id()) && aliasName.equals(actionEvent.getAliasName()));
        }
    }
}
