package com.tlc.components.starter.initializer.components;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.commons.util.Configurations;

import com.tlc.components.starter.fields.ComponentsStarterFields;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import com.tlc.search.SearchService;
import com.tlc.search.fields.ScriptKeys;
import com.tlc.search.internal.elasticsearch.ElasticSearchService;
import com.tlc.search.internal.opensearch.SelfManagedOpenSearch;
import com.tlc.search.internal.opensearch.ManagedOpenSearch;
import com.tlc.search.internal.registry.SearchServiceRegistry;
import com.tlc.search.service.provider.ScriptResourceRegistrar;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Responsible for triggering the initialization process of the search services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
final class SearchInitializer extends AbstractModuleInitializer {

    private final ServiceRegistry<SearchService> searchServiceRegistry;

    private SearchInitializer() {
        this.searchServiceRegistry = SearchServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        if (isServiceEnabled()) {
            final Configurations configurations = getProperties(ComponentsStarterFields.SEARCH);
            final Optional<SearchService> optionalSearchService = fetchSearchService(configurations.getConfig(ComponentsStarterFields.PROVIDER));

            optionalSearchService.ifPresent(searchService -> {
                searchService.start(configurations);
                searchServiceRegistry.register(searchService);
            });
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Registers the {@link com.tlc.search.service.ScriptResourceService} implementation
     * that uses {@link com.tlc.components.starter.scanner.ResourceScanner} to discover
     * individual {@code .painless} script files from the classpath.
     * </p>
     *
     * @throws Exception If an error occurs during post-initialization.
     */
    @Override
    public void postInitialize() {
        if (isServiceEnabled()) {
            ScriptResourceRegistrar.register(this::discoverScriptFiles);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return isServiceEnabled(ComponentsStarterFields.SEARCH);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ServiceRegistry<? extends Service> getServiceRegistry() {
        return searchServiceRegistry;
    }

    /**
     * <p>
     * Discovers all individual {@code .painless} script files from the {@code stored_scripts/} directory
     * across the classpath using {@link com.tlc.components.starter.scanner.ResourceScanner}.
     * </p>
     *
     * <p>
     * The script id is derived from the filename without the {@code .painless} extension.
     * </p>
     *
     * @return Map of script id to {@link InputStream} for each discovered script file.
     * @throws IOException If an I/O error occurs during resource discovery.
     */
    private Map<String, InputStream> discoverScriptFiles() throws IOException {
        final List<Resource> resources = resourceScanner.scanResources(ScriptKeys.SCRIPT_DIR_LOCATION, ScriptKeys.SCRIPT_FILE_EXTENSION);
        final Map<String, InputStream> scriptFiles = new LinkedHashMap<>();

        for (final Resource resource : resources) {
            if (resource.exists() && resource.isReadable()) {
                final String filename = resource.getFilename();

                if (filename != null && filename.endsWith(".painless")) {
                    final String scriptId = filename.substring(0, filename.length() - ".painless".length());

                    scriptFiles.put(scriptId, resource.getInputStream());
                }
            }
        }

        return scriptFiles;
    }

    /**
     * <p>
     * Fetches the {@link SearchService} based on the given search provider.
     * </p>
     *
     * @param searchProvider Represents the provider info of the search.
     * @return {@link SearchService}.
     */
    private Optional<SearchService> fetchSearchService(final String searchProvider) {
        return Optional.ofNullable(switch (searchProvider) {
            case "SelfManagedOpenSearch" -> new SelfManagedOpenSearch();
            case "ManagedOpenSearch" -> new ManagedOpenSearch();
            case "ElasticSearch" -> new ElasticSearchService();
            default -> null;
        });
    }
}
