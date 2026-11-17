package com.tlc.search.internal.opensearch;

import com.tlc.commons.util.Configurations;

import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;

/**
 * A managed open search service where the environment is entirely maintained and operated externally
 *
 * @author sundar
 * @version 1.0
 */
public class ManagedOpenSearch extends OpenSearchService {

    /**
     * {@inheritDoc}
     *
     * @param configurations Which contains the open search configuration
     * @return {@link OpenSearchTransport}
     */
    @Override
    protected OpenSearchTransport buildTransport(final Configurations configurations) {
        final Configurations managedConfigurations = configurations.getNestedConfigs("managed");
        final String openSearchServerlessDomain = managedConfigurations.getConfig("domain");
        final String signingService = managedConfigurations.getConfig("signing.service");
        final AwsCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();
        final SdkHttpClient sdkHttpClient = ApacheHttpClient.builder().build();
        final Region region = Region.of(managedConfigurations.getConfig("region"));

        return new AwsSdk2Transport(
                sdkHttpClient,
                openSearchServerlessDomain,
                signingService,
                region,
                AwsSdk2TransportOptions.builder().setCredentials(credentialsProvider).build()
        );
    }
}
