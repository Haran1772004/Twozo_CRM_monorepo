package com.tlc.search.internal.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.Configurations;

import com.tlc.search.SearchService;
import com.tlc.search.internal.elasticsearch.impl.ESClusterService;
import com.tlc.search.internal.elasticsearch.impl.ESIndexService;
import com.tlc.search.internal.elasticsearch.impl.ESOrgDocumentService;
import com.tlc.search.internal.elasticsearch.impl.ESScriptService;
import com.tlc.search.internal.elasticsearch.impl.ESWritableDocumentService;
import com.tlc.search.internal.status.IndexingErrorCodes;
import com.tlc.search.service.ClusterService;
import com.tlc.search.service.IndexService;
import com.tlc.search.service.OrgDocumentService;
import com.tlc.search.service.ReadableDocumentService;
import com.tlc.search.service.ScriptService;
import com.tlc.search.service.WritableDocumentService;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import java.io.File;

/**
 * <p>
 * Provides the service for pre searching process and searching the available data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class ElasticSearchService implements SearchService {

    private WritableDocumentService writableDocumentService;
    private IndexService indexService;
    private ClusterService clusterService;
    private ScriptService scriptService;
    private ElasticsearchClient client;

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        final String user = configurations.getConfig("user");
        final String password = configurations.getConfig("password");
        final String url = configurations.getConfig("url");
        final RestClientBuilder restClientBuilder = RestClient.builder(HttpHost.create(url));
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        final UsernamePasswordCredentials usernamePasswordCredentials = new UsernamePasswordCredentials(user, password);

        credentialsProvider.setCredentials(AuthScope.ANY, usernamePasswordCredentials);
        restClientBuilder.setHttpClientConfigCallback(httpAsyncClientBuilder -> {
            try {
                if (!Boolean.parseBoolean(configurations.getConfig("server.enabled"))) {
                    final String truststoreLocation = configurations.getConfig("truststore.location");
                    final String truststorePassword = configurations.getConfig("truststore.password");
                    final SSLContextBuilder contextBuilder = SSLContexts.custom();

                    contextBuilder.loadTrustMaterial(new File(truststoreLocation), truststorePassword.toCharArray());
                    httpAsyncClientBuilder.setSSLContext(contextBuilder.build());
                }
                httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                return httpAsyncClientBuilder;
            } catch (Exception e) {
                throw ErrorCode.get(IndexingErrorCodes.REST_CLIENT_CREATION_FAILED, e);
            }
        });
        client = new ElasticsearchClient(new RestClientTransport(restClientBuilder.build(), new JacksonJsonpMapper()));
        writableDocumentService = new ESWritableDocumentService(client);
        indexService = new ESIndexService(client);
        clusterService = new ESClusterService(client);
        scriptService = new ESScriptService(client);
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
    public WritableDocumentService getWritableService() {
        return writableDocumentService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadableDocumentService getReadableService(final Long orgId) {
        return new ESOrgDocumentService(orgId, client);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrgDocumentService getOrgDocumentService(final Long orgId) {
        return new ESOrgDocumentService(orgId, client);
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
}
