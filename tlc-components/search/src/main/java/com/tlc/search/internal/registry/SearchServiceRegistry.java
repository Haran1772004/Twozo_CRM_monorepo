package com.tlc.search.internal.registry;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.search.SearchService;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Registers the {@link SearchService} into the registry.
 *
 * @author Sathish
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class SearchServiceRegistry implements ServiceRegistry<SearchService> {

    private final AtomicReference<SearchService> searchService;

    private static final class Instance {
        private static final SearchServiceRegistry SEARCH_SERVICE_REGISTRY = new SearchServiceRegistry();
    }

    private SearchServiceRegistry() {
        this.searchService = new AtomicReference<>();
    }

    public static ServiceRegistry<SearchService> getInstance() {
        return Instance.SEARCH_SERVICE_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @param service The service to register.
     */
    @Override
    public void register(final SearchService service) {
        if (!searchService.compareAndSet(null, service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public SearchService get() {
        return searchService.get();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link SearchService}.
     */
    @Override
    public SearchService unregister() {
        return searchService.getAndSet(null);
    }
}
