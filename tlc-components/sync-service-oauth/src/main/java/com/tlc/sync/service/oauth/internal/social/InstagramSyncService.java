package com.tlc.sync.service.oauth.internal.social;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.sync.service.oauth.fields.SyncServiceFields;
import com.tlc.sync.service.oauth.internal.status.SyncErrorCodes;
import com.tlc.sync.service.oauth.model.request.TokenReadRequest;

import com.tlc.sync.service.oauth.model.request.facebook.FBTokenReadRequest;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import com.tlc.web.HttpUtil;
import com.tlc.web.client.common.UrlUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Facilitates instagram-sync related actions.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class InstagramSyncService implements SocialSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstagramSyncService.class);

    private final String uiDialogApiEndpoint;
    private final String clientId;
    private final String redirectUri;
    private final String scopes;
    private final Map<String, String> accessTokenParams;
    private final String graphApiEndpoint;
    private final String accessTokenUri;

    public InstagramSyncService(final String uiDialogApiEndpoint, final String graphApiEndpoint, final String accessTokenUri,
                                final String clientId, final String clientSecret, final String redirectUri, final String scopes) {
        this.uiDialogApiEndpoint = uiDialogApiEndpoint;
        this.graphApiEndpoint = graphApiEndpoint;
        this.accessTokenUri = accessTokenUri;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.scopes = scopes;
        this.accessTokenParams = Map.of(SyncServiceFields.CLIENT_ID, clientId, SyncServiceFields.CLIENT_SECRET, clientSecret);
    }

    /**
     * {@inheritDoc}
     *
     * @return the user consent uri
     */
    @Override
    public String getUserSyncConsentUri() {
        return String.format("%s/dialog/oauth?client_id=%s&redirect_uri=%s&scope=%s&response_type=code", uiDialogApiEndpoint, clientId, getUserSyncRedirectUri(), scopes);
    }

    /**
     * {@inheritDoc}
     *
     * @return the sync redirect uri
     */
    @Override
    public String getUserSyncRedirectUri() {
        return String.format("%s%s", getDomain(), redirectUri);
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     */
    @Override
    public String fetchAccessToken(final TokenReadRequest tokenReadRequest) {
        final FBTokenReadRequest fbTokenReadRequest = (FBTokenReadRequest) tokenReadRequest;
        final Map<String, String> tokenParams = new HashMap<>(accessTokenParams);

        tokenParams.put(SyncServiceFields.REDIRECT_URI, fbTokenReadRequest.getSyncUrlConfig().getRedirectUrl());
        tokenParams.put(SyncServiceFields.CODE, fbTokenReadRequest.getAccessCode());
        try {
            final String tokenURL = UrlUtil.appendQueryParams(graphApiEndpoint.concat(accessTokenUri), tokenParams);
            final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(tokenURL), Collections.emptyMap(), HttpMethod.GET, Collections.emptyMap()));

            return response.getString(SyncServiceFields.ACCESS_TOKEN);
        } catch (Exception exception) {
            LOGGER.error("Instagram: Access token fetch failed, Reason: {}", exception.getMessage());
            throw ErrorCode.get(SyncErrorCodes.INVALID_REFRESH_TOKEN);
        }
    }

    /**
     * <p>
     * Refreshes the provided access token and return the new token.
     * </p>
     *
     * @param fbTokenReadRequest Represents the {@link FBTokenReadRequest}.
     * @return the refreshed access token.
     */
    public String getRefreshedAccessToken(final FBTokenReadRequest fbTokenReadRequest) {
        final Map<String, String> tokenParams = new HashMap<>(accessTokenParams);
        final String accessToken = fbTokenReadRequest.getAccessToken();

        tokenParams.put(SyncServiceFields.GRANT_TYPE, SyncServiceFields.FB_EXCHANGE_TOKEN);
        tokenParams.put(SyncServiceFields.FB_EXCHANGE_TOKEN, accessToken);
        try {
            final String tokenURL = UrlUtil.appendQueryParams(graphApiEndpoint.concat(accessTokenUri), tokenParams);
            final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(tokenURL), Collections.emptyMap(), HttpMethod.GET, Collections.emptyMap()));

            return response.getString(SyncServiceFields.ACCESS_TOKEN);
        } catch (Exception exception) {
            LOGGER.error("Instagram: Failed to refresh the access token, Reason: {}", exception.getMessage());
            throw ErrorCode.get(SyncErrorCodes.INVALID_REFRESH_TOKEN);
        }
    }
}