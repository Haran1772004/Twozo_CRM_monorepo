package com.tlc.crm.entity.list.dynamic.container;

import com.tlc.crm.entity.list.ListFieldsFetcher;
import com.tlc.crm.entity.list.model.Provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * Registry for managing {@link ListFieldsFetcher} instances associated with different {@link Provider}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class ListDynamicFieldsFetcherRegistry {

    private final Map<Provider, ListFieldsFetcher<?>> dynamicFieldsFetchers;

    private ListDynamicFieldsFetcherRegistry() {
        dynamicFieldsFetchers = new ConcurrentHashMap<>();
    }

    private static class InstanceHolder {
        private static final ListDynamicFieldsFetcherRegistry INSTANCE = new ListDynamicFieldsFetcherRegistry();
    }

    /**
     * <p>
     * Gets the {@link ListDynamicFieldsFetcherRegistry} instance
     * </p>
     *
     * @return The {@link ListDynamicFieldsFetcherRegistry}
     */
    public static ListDynamicFieldsFetcherRegistry getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Adds the {@link ListFieldsFetcher} for the given {@link Provider}
     * </p>
     *
     * @param provider         Represents the {@link Provider}
     * @param listFieldsFetcher Represents the {@link ListFieldsFetcher}
     */
    public void add(final Provider provider, final ListFieldsFetcher<?> listFieldsFetcher) {
        dynamicFieldsFetchers.put(provider, listFieldsFetcher);
    }

    /**
     * <p>
     * Fetches the {@link ListFieldsFetcher} for the given {@link Provider}
     * </p>
     *
     * @param provider Represents the {@link Provider}
     * @return The {@link ListFieldsFetcher}
     */
    public ListFieldsFetcher<?> get(final Provider provider) {
        return dynamicFieldsFetchers.get(provider);
    }

    /**
     * <p>
     * Clears the {@link ListFieldsFetcher}
     * </p>
     */
    public void clear() {
        dynamicFieldsFetchers.clear();
    }
}
