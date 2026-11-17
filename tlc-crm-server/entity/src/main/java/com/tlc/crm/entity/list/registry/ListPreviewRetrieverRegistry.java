package com.tlc.crm.entity.list.registry;

import com.tlc.crm.entity.list.model.ListPreviewRetriever;
import com.tlc.crm.entity.list.model.Provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * Registry to store the provider with its list preview retriever
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class ListPreviewRetrieverRegistry {

    private final Map<Provider, ListPreviewRetriever> partialFieldsFetchers;

    private ListPreviewRetrieverRegistry() {
        partialFieldsFetchers = new ConcurrentHashMap<>();
    }

    private static class InstanceHolder {
        private static final ListPreviewRetrieverRegistry INSTANCE = new ListPreviewRetrieverRegistry();
    }

    /**
     * <p>
     * Gets the {@link ListPreviewRetrieverRegistry} instance
     * </p>
     *
     * @return The {@link ListPreviewRetrieverRegistry}
     */
    public static ListPreviewRetrieverRegistry getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Adds the {@link ListPreviewRetriever} for the given {@link Provider}
     * </p>
     *
     * @param provider             Represents the {@link Provider}
     * @param listPreviewRetriever Represents the {@link ListPreviewRetriever}
     */
    public void add(final Provider provider, final ListPreviewRetriever listPreviewRetriever) {
        partialFieldsFetchers.put(provider, listPreviewRetriever);
    }

    /**
     * <p>
     * Fetches the {@link ListPreviewRetriever} for the given {@link Provider}
     * </p>
     *
     * @param provider Represents the {@link Provider}
     * @return The {@link ListPreviewRetriever}
     */
    public ListPreviewRetriever get(final Provider provider) {
        return partialFieldsFetchers.get(provider);
    }

    /**
     * <p>
     * Clears the {@link ListPreviewRetriever}
     * </p>
     */
    public void clear() {
        partialFieldsFetchers.clear();
    }
}