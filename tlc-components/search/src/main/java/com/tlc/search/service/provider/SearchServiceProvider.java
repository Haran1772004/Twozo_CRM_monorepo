package com.tlc.search.service.provider;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.search.SearchService;
import com.tlc.search.internal.registry.SearchServiceRegistry;

import java.util.Objects;

/**
 * Provides the services that enable the {@link SearchService} to execute its functions effectively.
 *
 * @author Sathish
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class SearchServiceProvider {

    private static final ServiceRegistry<SearchService> SEARCH_SERVICE_REGISTRY;

    static {
        SEARCH_SERVICE_REGISTRY = SearchServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link SearchService}.
     */
    public static SearchService get() {
        final SearchService service = SEARCH_SERVICE_REGISTRY.get();

        if (Objects.isNull(service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service not initialized");
        } else {
            return service;
        }
    }
}
