package com.tlc.crm.workflow.cache.service.internal;

import com.tlc.crm.workflow.api.model.TriggerActionSource;

import com.tlc.crm.workflow.cache.resolver.TriggerActionSourceResolver;
import com.tlc.crm.workflow.cache.service.TriggerActionSourceService;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the implementation for the services offered by {@link TriggerActionSource}
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see TriggerActionSourceService
 */
public final class TriggerActionSourceServiceImpl implements TriggerActionSourceService {

    private final TriggerActionSourceResolver triggerActionSourceResolver;

    private TriggerActionSourceServiceImpl() {
        this.triggerActionSourceResolver = TriggerActionSourceResolver.getInstance();
    }

    private static class Instance {
        private static final TriggerActionSourceServiceImpl INSTANCE = new TriggerActionSourceServiceImpl();
    }

    /**
     * Gets the instance of {@link TriggerActionSourceServiceImpl}.
     *
     * @return The {@link TriggerActionSourceServiceImpl}.
     */
    public static TriggerActionSourceServiceImpl getInstance() {
        return TriggerActionSourceServiceImpl.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionSourceId Represents the trigger action source id.
     * @return The {@link TriggerActionSource}.
     */
    @Override
    public TriggerActionSource get(final Long triggerActionSourceId) {
        return triggerActionSourceResolver.get(triggerActionSourceId);
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of {@link TriggerActionSource}.
     */
    @Override
    public Collection<TriggerActionSource> getAll() {
        return triggerActionSourceResolver.getAll();
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionSourceId Represents the trigger action source id.
     * @return If the trigger action source exists it will return true, otherwise it will return false.
     */
    @Override
    public boolean exists(final Long triggerActionSourceId) {
        return Objects.nonNull(get(triggerActionSourceId));
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionSourceIds Which contains the collection of trigger action source ids.
     * @return If all the trigger action source exists it will return true, otherwise it will return false.
     */
    @Override
    public boolean exists(final Collection<Long> triggerActionSourceIds) {
        if (triggerActionSourceIds.isEmpty()) {
            return false;
        } else {
            final Collection<TriggerActionSource> triggerActionSources = getAll();

            return Objects.nonNull(triggerActionSources) && triggerActionSources.stream().map(TriggerActionSource::id)
                    .collect(Collectors.toSet()).containsAll(triggerActionSourceIds);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionSourceIds The collection of trigger action source ids
     * @param aliasName              The alias name to compare
     * @return {@code true} if the alias name matches any of the trigger action source ids, otherwise {@code false}
     */
    @Override
    public boolean isAliasNameMatch(final Collection<Long> triggerActionSourceIds, final String aliasName) {
        if (triggerActionSourceIds.isEmpty()) {
            return false;
        } else {
            final Collection<TriggerActionSource> triggerActionSources = getAll();

            return Objects.nonNull(triggerActionSources) && triggerActionSources.stream().anyMatch(source ->
                    triggerActionSourceIds.contains(source.id()) && aliasName.equals(source.getAliasName()));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionSourceId The id of the trigger action source for which to retrieve the alias name
     * @return The alias name corresponding to the provided trigger action source id
     */
    @Override
    public String getAliasName(final Long triggerActionSourceId) {
        return get(triggerActionSourceId).getAliasName();
    }

    /**
     * {@inheritDoc}
     *
     * @param aliasName Represents the alias name
     * @return The {@link TriggerActionSource} details
     */
    @Override
    public TriggerActionSource getActionSourceByAliasName(final String aliasName) {
        final Collection<TriggerActionSource> triggerActionSources = getAll();

        return Objects.isNull(triggerActionSources) ? null : triggerActionSources.stream().filter(actionSource ->
                Objects.equals(aliasName.toLowerCase(), actionSource.getAliasName())).findFirst().orElse(null);
    }
}
