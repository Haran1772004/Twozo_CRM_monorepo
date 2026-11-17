package com.tlc.sync.service.oauth.internal.calendar;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IClientCredential;
import com.microsoft.aad.msal4j.RefreshTokenParameters;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.sync.service.oauth.fields.SyncServiceFields;
import com.tlc.sync.service.oauth.internal.status.SyncErrorCodes;
import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.model.request.TokenReadRequest;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenReadRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenWriteRequest;
import com.tlc.sync.service.oauth.service.token.CalendarSyncTokenService;

import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import com.tlc.web.HttpUtil;

import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * Configures Outlook calendar properties for oauth
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public class OutlookCalendarSyncService implements CalendarSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutlookCalendarSyncService.class);

    private final String authority;
    private final String responseType;
    private final String clientId;
    private final String clientSecret;
    private final String commonAuthority;
    private final String accessTokenUri;
    private final String userConsentScope;
    private final String defaultScope;
    private final String userConsentRedirectUri;
    private final String prompt;
    private final Map<String, String> refreshTokenParams;
    private final Map<String, String> accessTokenParams;
    private final CalendarSyncTokenService calendarSyncTokenService;

    public OutlookCalendarSyncService(final String authority, final String responseType, final String clientId,
                                      final String userConsentScope, final String userConsentRedirectUri, final String grantType,
                                      final String clientSecret, final String accessTokenUri, final String defaultScope,
                                      final String commonAuthority, final String prompt) {
        this.authority = authority;
        this.responseType = responseType;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.userConsentScope = userConsentScope;
        this.userConsentRedirectUri = userConsentRedirectUri;
        this.accessTokenUri = accessTokenUri;
        this.commonAuthority = commonAuthority;
        this.defaultScope = defaultScope;
        this.prompt = prompt;
        this.accessTokenParams = Map.of(SyncServiceFields.CLIENT_ID, clientId, SyncServiceFields.CLIENT_SECRET, clientSecret, SyncServiceFields.SCOPE, defaultScope,
                SyncServiceFields.GRANT_TYPE, SyncServiceFields.REFRESH_TOKEN);
        this.refreshTokenParams = Map.of(SyncServiceFields.GRANT_TYPE, grantType, SyncServiceFields.CLIENT_ID, clientId, SyncServiceFields.CLIENT_SECRET, clientSecret);
        this.calendarSyncTokenService = CalendarSyncTokenService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return user consent uri {@link String}
     */
    @Override
    public String getUserSyncConsentUri() {
        return String.format("%s?response_type=%s&client_id=%s&scope=%s&redirect_uri=%s&prompt=%s",
                authority, responseType, clientId, userConsentScope, getUserSyncRedirectUri(), encodePrompt(prompt));
    }

    /**
     * {@inheritDoc}
     *
     * @return the user consent redirect uri
     */
    @Override
    public String getUserSyncRedirectUri() {
        return String.format("%s%s", getDomain(), userConsentRedirectUri);
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
            final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(accessTokenUri), requestParams, HttpMethod.POST, Collections.emptyMap()));
            final String refreshToken = response.getString(SyncServiceFields.REFRESH_TOKEN);
            final EmailAddress emailAddress = new EmailAddress(getEmailAddress(refreshToken));

            emailTokenWriteRequest.setToken(refreshToken);
            emailTokenWriteRequest.setEmailAddress(emailAddress);
            calendarSyncTokenService.putRefreshToken(emailTokenWriteRequest);
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Calendar sync: Refresh token fetch failed, Reason: {}", exception.getMessage());
            throw ErrorCode.get(SyncErrorCodes.INVALID_REFRESH_TOKEN, exception);
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
            LoggerUtil.logError(LOGGER, exception, "Calendar sync: Access token fetch failed, Reason: {}", exception.getMessage());
            throw ErrorCode.get(SyncErrorCodes.INVALID_ACCESS_TOKEN, exception);
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
            LoggerUtil.logError(LOGGER, throwable, "Calendar sync: User email address fetch failed, Reason: {}", throwable.getMessage());
            throw ErrorCode.get(SyncErrorCodes.EMAIL_ADDRESS_FETCH_FAILED, throwable);
        }
    }
}