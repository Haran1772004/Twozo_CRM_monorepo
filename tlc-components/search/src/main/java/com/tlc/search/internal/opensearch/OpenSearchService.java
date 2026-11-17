package com.tlc.search.internal.opensearch;

import com.tlc.commons.util.Configurations;
import com.tlc.search.SearchService;
import com.tlc.search.internal.opensearch.impl.OSClusterService;
import com.tlc.search.internal.opensearch.impl.OSIndexService;
import com.tlc.search.internal.opensearch.impl.OSOrgDocumentService;
import com.tlc.search.internal.opensearch.impl.OSScriptService;
import com.tlc.search.internal.opensearch.impl.OSWritableDocumentService;
import com.tlc.search.service.ClusterService;
import com.tlc.search.service.IndexService;
import com.tlc.search.service.OrgDocumentService;
import com.tlc.search.service.ReadableDocumentService;
import com.tlc.search.service.ScriptService;
import com.tlc.search.service.WritableDocumentService;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;

/**
 * <p>
 * Provides the service for pre searching process and searching the available data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
abstract class OpenSearchService implements SearchService {

    private static final String OPEN_SEARCH_CONFIG = "openSearch";

    private WritableDocumentService writableDocumentService;
    private IndexService indexService;
    private ClusterService clusterService;
    private ScriptService scriptService;
    private OpenSearchClient client;

    public OpenSearchService() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void start(final Configurations configurations) {
        this.client = new OpenSearchClient(buildTransport(configurations.getNestedConfigs(OPEN_SEARCH_CONFIG)));
        this.writableDocumentService = new OSWritableDocumentService(client);
        this.indexService = new OSIndexService(client);
        this.clusterService = new OSClusterService(client);
        this.scriptService = new OSScriptService(client);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        client.shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadableDocumentService getReadableService(final Long orgId) {
        return new OSOrgDocumentService(orgId, client);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrgDocumentService getOrgDocumentService(final Long orgId) {
        return new OSOrgDocumentService(orgId, client);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WritableDocumentService getWritableService() {
        return writableDocumentService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexService indexService() {
        return indexService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClusterService clusterService() {
        return clusterService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScriptService scriptService() {
        return scriptService;
    }

    /**
     * Builds the {@link OpenSearchTransport} using the configuration
     *
     * @param configurations Which contains the open search configuration
     * @return {@link OpenSearchTransport}
     */
    protected abstract OpenSearchTransport buildTransport(final Configurations configurations);
}
