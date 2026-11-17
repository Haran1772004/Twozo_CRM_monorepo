package com.tlc.crm.entity.list.filter.internal;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.entity.internal.status.FilterErrorCodes;

import com.tlc.crm.entity.list.filter.Filter;
import com.tlc.crm.entity.list.filter.FilterAction;
import com.tlc.crm.entity.list.filter.source.FilterSourceType;
import com.tlc.crm.entity.list.filter.source.SourceLinkGroup;
import com.tlc.crm.entity.list.model.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * <p>
 * A container which holds the filter action of entity of multiple groups.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FilterActionContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterActionContainer.class);

    private final Map<String, Map<FilterSourceType, FilterAction>> registeredSourceActions;
    private final Map<String, Map<Provider, FilterAction>> registeredProviderActions;
    private final Map<FilterSourceType, Collection<SourceLinkGroup>> sourceLinkGroups;
    private final Map<FilterAction, FilterSourceType> filterActionSource;

    private FilterActionContainer() {
        registeredSourceActions = new HashMap<>();
        registeredProviderActions = new HashMap<>();
        sourceLinkGroups = new HashMap<>();
        filterActionSource = new HashMap<>();
    }

    private static class Instance {
        private static final FilterActionContainer INSTANCE = new FilterActionContainer();
    }

    public static FilterActionContainer getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Adds the filter action into container.
     * </p>
     *
     * @param group        filter group.
     * @param filter       Holds the details of filter annotation.
     * @param filterAction {@link FilterAction}.
     */
    public void addAction(final String group, final Filter filter, final FilterAction filterAction) {
        final Map<FilterSourceType, FilterAction> sourceActions = registeredSourceActions.computeIfAbsent(group, map -> new HashMap<>());
        final Map<Provider, FilterAction> providerActions = registeredProviderActions.computeIfAbsent(group, map -> new HashMap<>());
        final FilterSourceType source = filter.source();
        final Provider provider = filter.provider();
        final FilterAction oldSourceFilterAction = sourceActions.get(source);
        final FilterAction oldProviderFilterAction = providerActions.get(provider);

        if (Objects.isNull(oldSourceFilterAction) && Objects.isNull(oldProviderFilterAction)) {
            LOGGER.info("Registering filter action for source {}", source);
            sourceActions.put(source, filterAction);
            filterActionSource.put(filterAction, source);

            if (Provider.isAllowedForFilter(provider)) {
                providerActions.put(provider, filterAction);
            }
        } else {
            throw ErrorCode.get(FilterErrorCodes.FILTER_ACTION_ALREADY_REGISTERED, String.format(" Source : %s", source));
        }
        loadSourceLinkGroupsContainer(source, filter.sourceLinkGroups());
    }

    /**
     * <p>
     * Loads the source link groups container.
     * </p>
     *
     * @param source For stores the source link groups.
     * @param sourceLinkGroups It provides the link between the two sources.
     */
    private void loadSourceLinkGroupsContainer(final FilterSourceType source, final SourceLinkGroup[] sourceLinkGroups) {
        if (sourceLinkGroups.length > 0) {
            final Collection<SourceLinkGroup> irrelevantSourceList = this.sourceLinkGroups.computeIfAbsent(source,
                    collection -> new ArrayList<>());

            irrelevantSourceList.addAll(Arrays.asList(sourceLinkGroups));
        }
    }

    /**
     * <p>
     * Fetches the source of the given {@link FilterAction}.
     * </p>
     *
     * @param filterAction for fetches the corresponding source.
     * @return source of the given filter action.
     */
    public FilterSourceType getSource(final FilterAction filterAction) {
        return filterActionSource.get(filterAction);
    }

    /**
     * <p>
     * Fetches the {@link FilterAction} of a source from container.
     * </p>
     *
     * @param source for fetches the corresponding {@link FilterAction}.
     * @return If filter action available for the given source,
     * it will return the action otherwise it will throw an error.
     */
    public FilterAction fetchSourceAction(final FilterSourceType source) {
        final Collection<Map<FilterSourceType, FilterAction>> sourceActions = registeredSourceActions.values();

        if (!sourceActions.isEmpty()) {
            for (final Map<FilterSourceType, FilterAction> sourceAction : sourceActions) {
                final FilterAction filterAction = sourceAction.get(source);

                if (Objects.nonNull(filterAction)) {
                    return filterAction;
                }
            }
            throw ErrorCode.get(FilterErrorCodes.FILTER_ACTION_NOT_FOUND);
        } else {
            throw ErrorCode.get(FilterErrorCodes.NO_FILTER_ACTIONS_REGISTERED);
        }
    }

    /**
     * <p>
     * Fetches the {@link FilterAction} of a provider from container.
     * </p>
     *
     * @param provider for fetches the corresponding {@link FilterAction}.
     * @return If filter action available for the given source,
     * it will return the action otherwise it will throw an error.
     */
    public FilterAction fetchProviderAction(final Provider provider) {
        final Collection<Map<Provider, FilterAction>> providerActions = registeredProviderActions.values();

        if (!providerActions.isEmpty()) {
            for (final Map<Provider, FilterAction> providerAction : providerActions) {
                final FilterAction filterAction = providerAction.get(provider);

                if (Objects.nonNull(filterAction)) {
                    return filterAction;
                }
            }
            throw ErrorCode.get(FilterErrorCodes.FILTER_ACTION_NOT_FOUND);
        } else {
            throw ErrorCode.get(FilterErrorCodes.NO_FILTER_ACTIONS_REGISTERED);
        }
    }

    /**
     * <p>
     * Fetches the collection of irrelevant source from container.
     * </p>
     *
     * @param source for fetches the corresponding irrelevant sources.
     * @return collection of irrelevant source.
     */
    public Collection<SourceLinkGroup> fetchSourceLinkGroups(final FilterSourceType source) {
        return sourceLinkGroups.get(source);
    }

    /**
     * <p>
     * Returns the count of registered filter actions in a group.
     * </p>
     *
     * @param groupName filter source.
     * @return count of registered actions for given group name.
     */
    public int count(final String groupName) {
        final Map<FilterSourceType, FilterAction> filterActions = registeredSourceActions.get(groupName);

        return Objects.isNull(filterActions) || filterActions.isEmpty() ? 0 : filterActions.size();
    }

    /**
     * <p>
     * Clears the filter actions registered in a group.
     * </p>
     *
     * @param groupName - Group name of filter action.
     */
    public void clear(final String groupName) {
        final Map<FilterSourceType, FilterAction> sourceActions = registeredSourceActions.remove(groupName);
        final Map<Provider, FilterAction> providerActions = registeredProviderActions.remove(groupName);

        if (Objects.nonNull(sourceActions) && !sourceActions.isEmpty()) {
            LOGGER.info("Total unregistered source filter actions count {}, group {} ", sourceActions.size(), groupName);

            for (final FilterSourceType filterSourceType : sourceActions.keySet()) {
                final Collection<SourceLinkGroup> sourceLinkGroups = this.sourceLinkGroups.get(filterSourceType);

                if (Objects.nonNull(sourceLinkGroups) && !sourceLinkGroups.isEmpty()) {
                    LOGGER.info("Total unregistered irrelevant source groups count {}, group {} ", sourceLinkGroups.size(), groupName);
                }
            }
        }

        if (Objects.nonNull(providerActions) && !providerActions.isEmpty()) {
            LOGGER.info("Total unregistered provider filter actions count {}, group {} ", providerActions.size(), groupName);
        }
    }

    /**
     * Removes all registered filter actions across all groups.
     */
    public void clearAll() {
        registeredSourceActions.clear();
        registeredProviderActions.clear();
        sourceLinkGroups.clear();
    }
}
