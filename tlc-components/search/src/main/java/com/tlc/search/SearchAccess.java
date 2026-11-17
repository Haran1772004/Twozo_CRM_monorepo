package com.tlc.search;

import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.commons.util.ConfHolder;

import com.tlc.commons.util.Configurations;
import com.tlc.search.internal.elasticsearch.ElasticSearchService;

import com.tlc.search.internal.registry.SearchServiceRegistry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.io.IOException;

/**
 * <p>
 * Activates the bundle and provides the access to the search services.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class SearchAccess implements BundleActivator {

    private final ServiceRegistry<SearchService> searchServiceRegistry = SearchServiceRegistry.getInstance();

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     * @throws IOException
     */
    @Override
    public void start(final BundleContext bundleContext) {
        final SearchService searchService = new ElasticSearchService();
        final Configurations configurations = ConfHolder.getInstance().getProperties("search");

        searchService.start(configurations);
        searchServiceRegistry.register(searchService);
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being stopped.
     */
    @Override
    public void stop(final BundleContext bundleContext) {
        final SearchService searchService = searchServiceRegistry.unregister();

        searchService.stop();
    }
}
