package com.tlc.crm.entity.list.summary.container;

import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.list.summary.ListSummaryFetcher;
import com.tlc.crm.entity.list.summary.model.ListSummary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * Provides the summary fetcher to fetch summary for the list API.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class ListSummaryFetcherRegistry {

    private final Map<Provider, Collection<ListSummaryFetcher<?>>> listSummaryFetchers;

    private ListSummaryFetcherRegistry() {
        listSummaryFetchers = new ConcurrentHashMap<>();
    }

    private static class Instance {
        private static final ListSummaryFetcherRegistry INSTANCE = new ListSummaryFetcherRegistry();
    }

    public static ListSummaryFetcherRegistry getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Adds the {@link ListSummaryFetcher} into the container.
     * </p>
     *
     * @param provider           Represents the list provider.
     * @param listSummaryFetcher Represents the {@link ListSummaryFetcher} to fetch the {@link ListSummary}.
     */
    public void addFetcher(final Provider provider, final ListSummaryFetcher<?> listSummaryFetcher) {
        listSummaryFetchers.computeIfAbsent(provider, dataProvider -> new ArrayList<>()).add(listSummaryFetcher);
    }

    /**
     * <p>
     * Provides the {@link ListSummaryFetcher} from the container.
     * </p>
     *
     * @param provider Represents the list provider.
     * @return {@link ListSummaryFetcher}.
     */
    public Collection<ListSummaryFetcher<?>> getFetchers(final Provider provider) {
        return listSummaryFetchers.get(provider);
    }

    /**
     * <p>
     * Clears the {@link ListSummaryFetcher} from the container.
     * </p>
     */
    public void clear() {
        listSummaryFetchers.clear();
    }
}