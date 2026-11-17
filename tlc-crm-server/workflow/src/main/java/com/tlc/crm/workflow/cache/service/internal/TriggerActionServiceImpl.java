package com.tlc.crm.workflow.cache.service.internal;

import com.tlc.crm.workflow.api.model.TriggerAction;
import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.cache.resolver.TriggerActionResolver;
import com.tlc.crm.workflow.cache.service.TriggerActionService;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the implementation for the services offered by {@link TriggerActionService}
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see TriggerActionService
 */
public final class TriggerActionServiceImpl implements TriggerActionService {

    private final TriggerActionResolver triggerActionResolver;

    private TriggerActionServiceImpl() {
        this.triggerActionResolver = TriggerActionResolver.getInstance();
    }

    private static class Instance {
        private static final TriggerActionServiceImpl INSTANCE = new TriggerActionServiceImpl();
    }

    /**
     * Gets the instance of {@link TriggerActionServiceImpl}.
     *
     * @return The {@link TriggerActionServiceImpl}.
     */
    public static TriggerActionServiceImpl getInstance() {
        return TriggerActionServiceImpl.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerSourceId Represents the trigger source id.
     * @return The {@link TriggerAction}.
     */
    @Override
    public TriggerAction get(final Short triggerSourceId) {
        return triggerActionResolver.get(triggerSourceId);
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerSourceId Represents the trigger source id.
     * @return A map containing trigger action source ids as keys and corresponding {@link TriggerActionSource} as values.
     */
    @Override
    public Collection<TriggerActionSource> getActionSourceByTriggerSource(final Short triggerSourceId) {
        final TriggerAction triggerAction = get(triggerSourceId);

        return Objects.isNull(triggerAction) ? List.of() : triggerAction.triggerActionSources();
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerSourceId Represents the trigger source id.
     * @return The collection of trigger action source ids.
     */
    @Override
    public Collection<Long> getTriggerActionSourceIds(final Short triggerSourceId) {
        final TriggerAction triggerAction = get(triggerSourceId);

        if (Objects.nonNull(triggerAction)) {
            final Collection<TriggerActionSource> triggerActionSources = triggerAction.triggerActionSources();

            return Objects.isNull(triggerActionSources) ? List.of() : triggerActionSources.stream().map(TriggerActionSource::id).collect(Collectors.toUnmodifiableSet());
        }

        return List.of();
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of {@link TriggerAction}.
     */
    @Override
    public Collection<TriggerAction> getAll() {
        return triggerActionResolver.getAll();
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerSourceId Represents the trigger source id.
     * @return If the trigger source exists it will return true, otherwise it will return false.
     */
    @Override
    public boolean exists(final Short triggerSourceId) {
        return Objects.nonNull(get(triggerSourceId));
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerSourceIds Which contains the collection of trigger action source ids.
     * @return If all the trigger source exists it will return true, otherwise it will return false.
     */
    @Override
    public boolean exists(final Set<Short> triggerSourceIds) {
        if (triggerSourceIds.isEmpty()) {
            return false;
        } else {
            final Collection<TriggerAction> triggerActions = getAll();

            return Objects.nonNull(triggerActions) && triggerActions.stream().map(TriggerAction::sourceId)
                    .collect(Collectors.toUnmodifiableSet()).containsAll(triggerSourceIds);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerSourceId        Represents the trigger source id
     * @param triggerActionSourceIds The collection of trigger action source ids
     * @param aliasName              The alias name to compare
     * @return {@code true} if the alias name matches any of the trigger action source ids or the trigger source id, otherwise {@code false}
     */
    @Override
    public boolean aliasNameExists(final Short triggerSourceId, final Collection<Long> triggerActionSourceIds, final String aliasName) {
        return !triggerActionSourceIds.isEmpty() && getActionSourceByTriggerSource(triggerSourceId).stream()
                .anyMatch(source -> triggerActionSourceIds.contains(source.id()) && aliasName.equals(source.getAliasName()));
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerSourceId       Represents the trigger source id
     * @param triggerActionSourceId Represents the trigger action source id
     * @param triggerActionEventId  Represents the trigger action event id
     * @return True if the event id exists for the specified trigger source, trigger action source; otherwise, false
     */
    @Override
    public boolean isEventIdExists(final short triggerSourceId, final long triggerActionSourceId, final long triggerActionEventId) {
        return getActionSourceByTriggerSource(triggerSourceId).stream().filter(actionSource -> Objects.equals(triggerActionSourceId, actionSource.id())).findFirst()
                .map(actionSource -> actionSource.getTriggerActionEvents().stream()
                        .anyMatch(actionEvent -> Objects.equals(triggerActionEventId, actionEvent.id()))).orElse(false);
    }
}
