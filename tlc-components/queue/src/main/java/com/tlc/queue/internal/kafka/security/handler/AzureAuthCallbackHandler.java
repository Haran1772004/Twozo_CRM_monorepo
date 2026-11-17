package com.tlc.queue.internal.kafka.security.handler;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.exception.ClientAuthenticationException;
import com.azure.identity.CredentialUnavailableException;
import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.queue.internal.kafka.security.CustomOAuthBearerToken;
import com.tlc.queue.internal.resource.QueueErrorCodes;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Authentication callback handler for establishing a secure connection
 * between the Kafka client and the Azure Event Hubs service.
 *
 * @author sundar
 * @version 1.0
 */
public class AzureAuthCallbackHandler implements AuthenticateCallbackHandler {

    private final String scopeName;
    private final String principalName;

    public AzureAuthCallbackHandler() {
        final Configurations queueConfigurations = ConfHolder.getInstance().getProperties("queue");
        final Configurations configurations = queueConfigurations.getNestedConfigs("kafka.managed.azure.sasl.auth.callback");

        this.scopeName = configurations.getConfig("scope");
        this.principalName = configurations.getConfig("principal");
    }

    @Override
    public void configure(final Map<String, ?> map, final String s, final List<AppConfigurationEntry> list) {

    }

    @Override
    public void close() {

    }

    /**
     * Performs authentication using the managed identity credential, ensuring that
     * the Kafka client operates under a managed identity associated with the service.
     *
     * @param callbacks The callbacks used for the authentication process.
     * @throws IOException                  If an I/O error occurs during authentication.
     * @throws UnsupportedCallbackException If a provided callback is not supported.
     */
    @Override
    public void handle(final Callback[] callbacks) throws IOException, UnsupportedCallbackException {

        if (Objects.nonNull(callbacks)) {
            final Optional<AccessToken> optionalAccessToken = fetchAccessToken();

            for (final Callback callback : callbacks) {

                if (callback instanceof final OAuthBearerTokenCallback oAuthBearerTokenCallback) {

                    optionalAccessToken.ifPresentOrElse(accessToken -> {
                        final OAuthBearerToken kafkaToken = new CustomOAuthBearerToken(
                                accessToken.getToken(),
                                Set.of(scopeName),
                                accessToken.getExpiresAt().toInstant().toEpochMilli(),
                                principalName,
                                Instant.now().toEpochMilli()
                        );

                        oAuthBearerTokenCallback.token(kafkaToken);
                    }, () -> {
                        throw ErrorCode.get(QueueErrorCodes.AZURE_ACCESS_TOKEN_NOT_FOUND);
                    });
                } else {
                    throw ErrorCode.get(QueueErrorCodes.UNSUPPORTED_AUTHENTICATION_CALLBACK);
                }
            }
        }
    }

    /**
     * Fetches the access token from Azure
     *
     * @return the access token
     */
    private Optional<AccessToken> fetchAccessToken() {

        try {
            final TokenRequestContext requestContext = new TokenRequestContext()
                    .setScopes(List.of(scopeName));
            final ManagedIdentityCredential credential = new ManagedIdentityCredentialBuilder().build();

            return credential.getToken(requestContext).blockOptional();
        } catch (CredentialUnavailableException e) {
            throw ErrorCode.get(QueueErrorCodes.AZURE_MANAGED_IDENTITY_CREDENTIAL_AUTHENTICATION_UNAVAILABLE, e);
        } catch (ClientAuthenticationException e) {
            throw ErrorCode.get(QueueErrorCodes.AZURE_AUTHENTICATION_ERROR, e);
        } catch (Throwable e) {
            throw ErrorCode.get(QueueErrorCodes.AZURE_ACCESS_TOKEN_FETCH_FAILED, e);
        }
    }
}