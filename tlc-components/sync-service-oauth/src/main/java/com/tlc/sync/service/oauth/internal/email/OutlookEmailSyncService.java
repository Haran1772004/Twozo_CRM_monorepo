package com.tlc.sync.service.oauth.internal.email;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IClientCredential;
import com.microsoft.aad.msal4j.RefreshTokenParameters;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.StringUtil;
import com.tlc.sync.service.oauth.fields.SyncServiceFields;
import com.tlc.sync.service.oauth.internal.status.SyncErrorCodes;
import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.model.request.TokenReadRequest;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenReadRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenWriteRequest;
import com.tlc.sync.service.oauth.service.EmailSyncService;
import com.tlc.sync.service.oauth.service.token.EmailSyncTokenService;

import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import com.tlc.web.HttpUtil;

import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * Configures outlook properties for oauth
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class OutlookEmailSyncService implements EmailSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutlookEmailSyncService.class);

    private final String authority;
    private final String responseType;
    private final String clientId;
    private final String clientSecret;
    private final String userConsentScope;
    private final String userConsentRedirectUri;
    private final String aliasId;
    private final String accessTokenUri;
    private final String commonAuthority;
    private final String tenantAuthority;
    private final String clientCredentialId;
    private final String clientCredentialSecret;
    private final String defaultScope;
    private final String refreshTokenRotationScope;
    private final String prompt;
    private final Map<String, String> refreshTokenParams;
    private final Map<String, String> accessTokenParams;
    private final EmailSyncTokenService emailSyncTokenService;
    private final Collection<String> requiredScopes;

    public OutlookEmailSyncService(final String authority, final String responseType, final String clientId,
                                   final String userConsentScope, final String userConsentRedirectUri, final String grantType,
                                   final String clientSecret, final String accessTokenUri, final String defaultScope,
                                   final String commonAuthority, final String tenantAuthority, final String aliasId,
                                   final String clientCredentialId, final String clientCredentialSecret,
                                   final String refreshTokenRotationScope, final String prompt) {
        this.authority = authority;
        this.responseType = responseType;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.userConsentScope = userConsentScope;
        this.userConsentRedirectUri = userConsentRedirectUri;
        this.aliasId = aliasId;
        this.accessTokenUri = accessTokenUri;
        this.commonAuthority = commonAuthority;
        this.tenantAuthority = tenantAuthority;
        this.clientCredentialId = clientCredentialId;
        this.clientCredentialSecret = clientCredentialSecret;
        this.defaultScope = defaultScope;
        this.refreshTokenRotationScope = refreshTokenRotationScope;
        this.prompt = prompt;
        this.refreshTokenParams = Map.of(SyncServiceFields.GRANT_TYPE, grantType, SyncServiceFields.CLIENT_ID, clientId, SyncServiceFields.CLIENT_SECRET, clientSecret,
                SyncServiceFields.SCOPE, defaultScope);
        this.accessTokenParams = Map.of(SyncServiceFields.CLIENT_ID, clientId, SyncServiceFields.CLIENT_SECRET, clientSecret, SyncServiceFields.SCOPE, defaultScope,
                SyncServiceFields.GRANT_TYPE, SyncServiceFields.REFRESH_TOKEN);
        this.emailSyncTokenService = EmailSyncTokenService.getInstance();
        final Collection<String> requiredScopes = StringUtil.splitAndNormalize(userConsentScope, WHITE_SPACE_REGEX);

        requiredScopes.remove(SyncServiceFields.OFFLINE_ACCESS);
        this.requiredScopes = requiredScopes;
    }

    /**
     * {@inheritDoc}
     *
     * @return consent uri {@link String}
     */
    @Override
    public String getUserSyncConsentUri() {
        return String.format("%s?response_type=%s&client_id=%s&scope=%s&redirect_uri=%s&prompt=%s",
                authority, responseType, clientId, userConsentScope, getUserSyncRedirectUri(), encodePrompt(prompt));
    }

    /**
     * {@inheritDoc}
     *
     * @return Redirect Uri
     */
    @Override
    public String getUserSyncRedirectUri() {
        return String.format("%s%s", getDomain(), userConsentRedirectUri);
    }

    /**
     * {@inheritDoc}
     *
     * @return the map of user ids and emails in tenant
     */
    @Override
    public Map<String, EmailAddress> fetchAccounts() {
        final Optional<String> accessToken = fetchClientCredentialToken();

        if (accessToken.isPresent()) {
            final String clientCredentialToken = accessToken.get();

            try {
                final GraphServiceClient<Request> graphClient = GraphServiceClient.builder().authenticationProvider((token) ->
                        CompletableFuture.completedFuture(clientCredentialToken)).buildClient();
                final String requestUrl = graphClient.users().buildRequest().getRequestUrl().toString();
                final Map<String, String> headers = new HashMap<>();

                headers.put(SyncServiceFields.AUTHORIZATION, String.format("%s%s", SyncServiceFields.BEARER, clientCredentialToken));
                headers.put(SyncServiceFields.ACCEPT, SyncServiceFields.APPLICATION_JSON);
                headers.put(SyncServiceFields.CONTENT_TYPE, SyncServiceFields.APPLICATION_JSON);
                final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(requestUrl), Collections.emptyMap(), HttpMethod.GET, headers));
                final JsonArray jsonArray = response.optJsonArray(SyncServiceFields.VALUE);
                final Map<String, EmailAddress> emailAddresses = new HashMap<>();

                for (int index = 0; index < jsonArray.size(); index++) {
                    final JsonObject jsonObject = jsonArray.getJsonObject(index);
                    final String id = jsonObject.getString(SyncServiceFields.ID);

                    if (aliasId.equals(id)) {
                        emailAddresses.put(id, new EmailAddress(jsonObject.getString(SyncServiceFields.MAIL)));
                        break;
                    }
                }

                return emailAddresses;
            } catch (Throwable throwable) {
                LOGGER.debug("Email sync: User fetch failed from tenant, Reason{} :", throwable.getMessage());
                throw ErrorCode.get(SyncErrorCodes.INVALID_USER, throwable);
            }
        }
        return Collections.emptyMap();
    }


    /**
     * {@inheritDoc}
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     */
    @Override
    public void fetchAndStoreRefreshToken(final TokenWriteRequest tokenWriteRequest) {
        final EmailTokenWriteRequest emailTokenWriteRequest = (EmailTokenWriteRequest) tokenWriteRequest;

        try {
            final Map<String, String> requestParams = new HashMap<>(refreshTokenParams);

            requestParams.put(SyncServiceFields.REDIRECT_URI, emailTokenWriteRequest.getSyncUrlConfig().getRedirectUrl());
            requestParams.put(SyncServiceFields.CODE, emailTokenWriteRequest.getAccessCode());
            final JsonObject tokenResponse = HttpUtil.makeRequest(new HttpRequest(new URL(accessTokenUri),
                    requestParams, HttpMethod.POST, Collections.emptyMap()));

            validateTokenScopes(tokenResponse.getString(SyncServiceFields.SCOPE), requiredScopes);
            final String refreshToken = tokenResponse.getString(SyncServiceFields.REFRESH_TOKEN);
            final EmailAddress emailAddress = new EmailAddress(getEmailAddress(refreshToken));

            emailTokenWriteRequest.setToken(refreshToken);
            emailTokenWriteRequest.setEmailAddress(emailAddress);
            emailSyncTokenService.putRefreshToken(emailTokenWriteRequest);
        } catch (final Exception exception) {
            final String message = exception.getMessage();

            if (message.contains(SyncErrorCodes.INSUFFICIENT_PERMISSIONS.name())) {
                LOGGER.debug("Email sync: Refresh token lacks required permissions, Reason{} : ", message);
                throw ErrorCode.get(SyncErrorCodes.INSUFFICIENT_PERMISSIONS);
            }
            LOGGER.debug("Email sync: Refresh token fetch failed, Reason: {}", message);
            throw ErrorCode.get(SyncErrorCodes.INVALID_REFRESH_TOKEN);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     */
    @Override
    public String fetchAccessToken(final TokenReadRequest tokenReadRequest) {
        final EmailTokenReadRequest emailTokenReadRequest = (EmailTokenReadRequest) tokenReadRequest;
        final String refreshToken = emailTokenReadRequest.getRefreshToken();
        try {
            final Map<String, String> requestParams = new HashMap<>(accessTokenParams);

            requestParams.put(SyncServiceFields.REFRESH_TOKEN, refreshToken);
            final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(accessTokenUri), requestParams, HttpMethod.POST, Collections.emptyMap()));

            return response.getString(SyncServiceFields.ACCESS_TOKEN);
        } catch (Exception exception) {
            LOGGER.debug("Email sync: Access token fetch failed, Reason: {}", exception.getMessage());
            throw ErrorCode.get(SyncErrorCodes.INVALID_ACCESS_TOKEN);
        }
    }

    /**
     * <p>
     * Gets new refresh token by using existing refresh token
     * </p>
     *
     * @param oldRefreshToken  Represents the refresh token
     * @return the new refresh token
     */
    @Override
    public String getRefreshToken(final String oldRefreshToken) {
        try {
            final Map<String, String> tokenParams = new HashMap<>(refreshTokenParams);

            tokenParams.put(SyncServiceFields.SCOPE, refreshTokenRotationScope);
            tokenParams.put(SyncServiceFields.REFRESH_TOKEN, oldRefreshToken);
            tokenParams.put(SyncServiceFields.GRANT_TYPE, SyncServiceFields.REFRESH_TOKEN);
            final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(accessTokenUri), tokenParams, HttpMethod.POST, Collections.emptyMap()));

            return response.getString(SyncServiceFields.REFRESH_TOKEN);
        } catch (Throwable throwable) {
            LOGGER.debug("Email sync: Token refresh process failed, message: {}, trace: {}", throwable.getMessage());

            throw ErrorCode.get(SyncErrorCodes.INVALID_REFRESH_TOKEN, throwable);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return the access token got by using the client credential
     */
    @Override
    public Optional<String> fetchClientCredentialToken() {
        try {
            final ConfidentialClientApplication application = ConfidentialClientApplication.builder(clientCredentialId,
                    ClientCredentialFactory.createFromSecret(clientCredentialSecret)).authority(tenantAuthority).build();
            final ClientCredentialParameters clientCredentialParam = ClientCredentialParameters.builder(
                    Collections.singleton(defaultScope)).build();

            return Optional.ofNullable(application.acquireToken(clientCredentialParam).get().accessToken());
        } catch (Exception exception) {
            LOGGER.debug("Email sync: Client credential token fetch failed, Reason: {}", exception.getMessage());
            throw ErrorCode.get(SyncErrorCodes.BAD_REQUEST_ERROR, exception);
        }
    }

    /**
     * <p>
     * Gets the email id by using the refresh token
     * </p>
     *
     * @param refreshToken Represents the refresh token
     */
    private String getEmailAddress(final String refreshToken) {
        try {
            final IClientCredential credential = ClientCredentialFactory.createFromSecret(clientSecret);
            final ConfidentialClientApplication application = ConfidentialClientApplication.builder(clientId, credential)
                    .authority(commonAuthority).build();
            final RefreshTokenParameters refreshTokenParameters = RefreshTokenParameters.builder(Collections.singleton(defaultScope), refreshToken).build();
            final IAuthenticationResult authenticationResult = application.acquireToken(refreshTokenParameters).get();
            final GraphServiceClient<Request> graphClient = GraphServiceClient
                    .builder()
                    .authenticationProvider((token) -> CompletableFuture.completedFuture(authenticationResult.accessToken()))
                    .buildClient();
            final User user = graphClient.me().buildRequest().select(SyncServiceFields.MAIL).get();

            if (Objects.nonNull(user)) {
                final String emailAddress = user.mail;

                return Objects.nonNull(emailAddress) ? emailAddress : user.userPrincipalName;
            } else {
                throw ErrorCode.get(SyncErrorCodes.INVALID_USER);
            }
        } catch (Throwable throwable) {
            LOGGER.debug("Email sync: User email address fetch failed, Reason: {}", throwable.getMessage());
            throw ErrorCode.get(SyncErrorCodes.EMAIL_ADDRESS_FETCH_FAILED);
        }
    }
}