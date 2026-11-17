package com.tlc.sync.service.oauth.internal.email;

import com.tlc.commons.code.ErrorCode;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Configures Google Mail properties for oauth
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class GoogleEmailSyncService implements EmailSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleEmailSyncService.class);

    private final String authority;
    private final String responseType;
    private final String clientId;
    private final String userScope;
    private final String userRedirectUri;
    private final String accessTokenUri;
    private final String accessTokenInfoUri;
    private final String prompt;
    private final Map<String, String> refreshTokenParams;
    private final Map<String, String> accessTokenParams;
    private final EmailSyncTokenService emailSyncTokenService;
    private final Collection<String> requiredScopes;

    public GoogleEmailSyncService(final String authority, final String responseType, final String clientId, final String userScope,
                                  final String userRedirectUri, final String grantType, final String clientSecret, final String accessTokenUri,
                                  final String accessTokenInfoUri, final String prompt) {
        this.authority = authority;
        this.responseType = responseType;
        this.clientId = clientId;
        this.userScope = userScope;
        this.userRedirectUri = userRedirectUri;
        this.accessTokenUri = accessTokenUri;
        this.accessTokenInfoUri = accessTokenInfoUri;
        this.prompt = prompt;
        this.accessTokenParams = Map.of(SyncServiceFields.CLIENT_ID, clientId, SyncServiceFields.CLIENT_SECRET, clientSecret, SyncServiceFields.GRANT_TYPE, SyncServiceFields.REFRESH_TOKEN);
        this.refreshTokenParams = Map.of(SyncServiceFields.GRANT_TYPE, grantType, SyncServiceFields.CLIENT_ID, clientId, SyncServiceFields.CLIENT_SECRET, clientSecret);
        this.emailSyncTokenService = EmailSyncTokenService.getInstance();
        final Collection<String> requiredScopes = StringUtil.splitAndNormalize(userScope, WHITE_SPACE_REGEX);

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
        return String.format("%s?prompt=%s&access_type=offline&response_type=%s&client_id=%s&scope=%s&redirect_uri=%s",
                authority, encodePrompt(prompt), responseType, clientId, userScope, getUserSyncRedirectUri());
    }

    /**
     * {@inheritDoc}
     *
     * @return Redirect Uri
     */
    @Override
    public String getUserSyncRedirectUri() {
        return String.format("%s%s", getDomain(), userRedirectUri);
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
            final JsonObject tokenResponse = HttpUtil.makeRequest(new HttpRequest(new URL(accessTokenUri), requestParams, HttpMethod.POST, Collections.emptyMap()));

            validateTokenScopes(tokenResponse.getString(SyncServiceFields.SCOPE), requiredScopes);
            final EmailAddress emailAddress = getEmailAddress(tokenResponse.getString(SyncServiceFields.ID_TOKEN));

            emailTokenWriteRequest.setToken(tokenResponse.getString(SyncServiceFields.REFRESH_TOKEN));
            emailTokenWriteRequest.setEmailAddress(emailAddress);
            emailSyncTokenService.putRefreshToken(emailTokenWriteRequest);
        } catch (final Exception exception) {
            final String message = exception.getMessage();

            if (message.contains(SyncErrorCodes.INSUFFICIENT_PERMISSIONS.name())) {
                LoggerUtil.logError(LOGGER, exception, "Email sync: Refresh token lacks required permissions, Reason{} : ", message);
                throw ErrorCode.get(SyncErrorCodes.INSUFFICIENT_PERMISSIONS);
            }
            LoggerUtil.logError(LOGGER, exception, "Email sync: Refresh token fetch failed, Reason: {}", message);
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
            final JsonObject accessTokenResponse = HttpUtil.makeRequest(new HttpRequest(new URL(accessTokenUri), requestParams, HttpMethod.POST, Collections.emptyMap()));

            return accessTokenResponse.getString(SyncServiceFields.ACCESS_TOKEN);
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Email sync: Access token fetch failed, Reason: {}", exception.getMessage());
            throw ErrorCode.get(SyncErrorCodes.INVALID_ACCESS_TOKEN);
        }
    }

    /**
     * <p>
     * Retrieves the email address using the provided id token.
     * </p>
     *
     * @param idToken Represents the ID token of the user.
     * @return {@link EmailAddress} representing the user's email address.
     */
    private EmailAddress getEmailAddress(final String idToken) {
        try {
            final Map<String, String> headers = new HashMap<>();

            headers.put(SyncServiceFields.ID_TOKEN, idToken);
            final String url = String.format("%s?%s", accessTokenInfoUri, String.format("%s%s%s", SyncServiceFields.ID_TOKEN, "=", idToken));
            final JsonObject emailIdResponse = HttpUtil.makeRequest(new HttpRequest(new URL(url), Collections.emptyMap(), HttpMethod.GET, headers));

            return new EmailAddress(emailIdResponse.getString(SyncServiceFields.EMAIL));
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Email sync: Email address fetch failed, Reason: {}", exception.getMessage());
            throw ErrorCode.get(SyncErrorCodes.EMAIL_ADDRESS_FETCH_FAILED);
        }
    }
}