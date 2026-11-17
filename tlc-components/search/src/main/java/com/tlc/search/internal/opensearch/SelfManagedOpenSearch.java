package com.tlc.search.internal.opensearch;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.Configurations;

import com.tlc.search.internal.status.IndexingErrorCodes;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;

import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;

import java.util.Objects;

/**
 * A self-managed open search service where the environment is entirely maintained and operated independently
 *
 * @author sundar
 * @version 1.0
 */
public class SelfManagedOpenSearch extends OpenSearchService {

    /**
     * {@inheritDoc}
     *
     * @param configurations Which contains the open search configuration
     * @return {@link OpenSearchTransport}
     */
    @Override
    protected OpenSearchTransport buildTransport(final Configurations configurations) {
        final Configurations selfManagedConfigurations = configurations.getNestedConfigs("selfManaged");
        final String userName = selfManagedConfigurations.getConfig("user");
        final String password = selfManagedConfigurations.getConfig("password");
        final String urls = selfManagedConfigurations.getConfig("url");

        if (Objects.nonNull(urls)) {
            final String[] urlArray = urls.split(",");
            final HttpHost[] hosts = new HttpHost[urlArray.length];

            for (int index = 0; index < urlArray.length; index++) {
                hosts[index] = HttpHost.create(urlArray[index].trim());
            }
            final RestClientBuilder restClientBuilder = RestClient.builder(hosts);

            if (!"false".equals(selfManagedConfigurations.optConfig("security.enabled", "true"))) {
                final UsernamePasswordCredentials usernamePasswordCredentials = new UsernamePasswordCredentials(userName, password);
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

                credentialsProvider.setCredentials(AuthScope.ANY, usernamePasswordCredentials);
                restClientBuilder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
            }
            restClientBuilder.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setSocketTimeout(50000));

            return new RestClientTransport(restClientBuilder.build(), new JacksonJsonpMapper());
        } else {
            throw ErrorCode.get(IndexingErrorCodes.REST_CLIENT_CREATION_FAILED);
        }
    }
}