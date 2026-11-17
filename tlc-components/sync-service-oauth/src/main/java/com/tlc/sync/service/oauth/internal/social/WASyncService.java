package com.tlc.sync.service.oauth.internal.social;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.sync.service.oauth.fields.SyncServiceFields;
import com.tlc.sync.service.oauth.internal.status.SyncErrorCodes;
import com.tlc.sync.service.oauth.model.request.TokenReadRequest;
import com.tlc.sync.service.oauth.model.request.whatsapp.WATokenReadRequest;

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
 * A service implementation for handling Whatsapp sync service. It configures all the properties need for the authentication
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WASyncService implements SocialSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WASyncService.class);

    private final String graphApiEndpoint;
    private final Map<String, String> accessTokenParams;
    private final String accessTokenUri;

    public WASyncService(final String graphApiEndpoint, final String accessTokenUri, final String clientId, final String clientSecret) {
        this.graphApiEndpoint = graphApiEndpoint;
        this.accessTokenUri = accessTokenUri;
        this.accessTokenParams = Map.of(SyncServiceFields.CLIENT_ID, clientId, SyncServiceFields.CLIENT_SECRET, clientSecret);
    }

    /**
     * {@inheritDoc}
     *
     * @return the consent uri
     */
    @Override
    public String getUserSyncConsentUri() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @return redirect uri
     */
    @Override
    public String getUserSyncRedirectUri() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     * @return the access token
     */
    @Override
    public String fetchAccessToken(final TokenReadRequest tokenReadRequest) {
        final WATokenReadRequest waTokenReadRequest = (WATokenReadRequest) tokenReadRequest;
        final Map<String, String> queryParams = new HashMap<>(accessTokenParams);

        queryParams.put(SyncServiceFields.CODE, waTokenReadRequest.getAccessCode());
        try {
            final String tokenURL = UrlUtil.appendQueryParams(graphApiEndpoint.concat(accessTokenUri), queryParams);
            final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(tokenURL), Collections.emptyMap(), HttpMethod.GET, Collections.emptyMap()));

            return response.getString(SyncServiceFields.ACCESS_TOKEN);
        } catch (Exception exception) {
            LOGGER.error("Whatsapp: Access token fetch failed, Reason: {}", exception.getMessage());
            throw ErrorCode.get(SyncErrorCodes.INVALID_ACCESS_TOKEN);
        }
    }

    /**
     * <p>
     * Refreshes the provided access token and return the new token
     * </p>
     *
     * @param waTokenReadRequest Represents the {@link WATokenReadRequest}
     * @return the refreshed access token
     */
    public String getRefreshedAccessToken(final WATokenReadRequest waTokenReadRequest) {
        final Map<String, String> queryParams = new HashMap<>(accessTokenParams);
        final String accessToken = waTokenReadRequest.getAccessToken();

        queryParams.put(SyncServiceFields.GRANT_TYPE, SyncServiceFields.FB_EXCHANGE_TOKEN);
        queryParams.put(SyncServiceFields.FB_EXCHANGE_TOKEN, accessToken);
        try {
            final String tokenURL = UrlUtil.appendQueryParams(graphApiEndpoint.concat(accessTokenUri), queryParams);
            final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(tokenURL), Collections.emptyMap(), HttpMethod.GET, Collections.emptyMap()));

            return response.getString(SyncServiceFields.ACCESS_TOKEN);
        } catch (Exception exception) {
            LOGGER.error("Whatsapp: Failed to refresh the access token, Reason: {}", exception.getMessage());
            throw ErrorCode.get(SyncErrorCodes.INVALID_REFRESH_TOKEN);
        }
    }
}