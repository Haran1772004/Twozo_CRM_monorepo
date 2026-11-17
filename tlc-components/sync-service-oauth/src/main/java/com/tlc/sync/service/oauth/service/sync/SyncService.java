package com.tlc.sync.service.oauth.service.sync;

import com.tlc.commons.executor.context.session.SessionContext;
import com.tlc.commons.executor.context.session.SessionContextHolder;
import com.tlc.sync.service.oauth.model.SyncUrlConfig;
import com.tlc.sync.service.oauth.model.provider.Provider;
import com.tlc.sync.service.oauth.model.request.TokenReadRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * <p>
 * Performs the sync configuration related functions
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface SyncService {

    /**
     * <p>
     * Gets the user consent uri for authentication
     * </p>
     *
     * @return The url required for consent
     */
    String getUserSyncConsentUri();

    /**
     * <p>
     * Gets the redirect uri that is required for getting data after accepting consent
     * </p>
     *
     * @return The redirect uri provided
     */
    String getUserSyncRedirectUri();

    /**
     * <p>
     * Fetches the access token from the provided {@link TokenReadRequest}
     * </p>
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     */
    String fetchAccessToken(final TokenReadRequest tokenReadRequest);

    /**
     * <p>
     * Gets the url details for getting sync consent
     * </p>
     *
     * @param providerType Represents the provider type
     * @return {@link SyncUrlConfig}
     */
    static SyncUrlConfig getSyncUrlDetails(final Provider providerType) {
        final SyncService syncService = providerType.getSyncService();
        final SyncUrlConfig syncUrlConfig = new SyncUrlConfig();

        syncUrlConfig.setConsentUrl(syncService.getUserSyncConsentUri());
        syncUrlConfig.setRedirectUrl(syncService.getUserSyncRedirectUri());

        return syncUrlConfig;
    }

    /**
     * Gets the domain of current request
     *
     * @return the domain of current request
     */
    default String getDomain() {
        final Optional<SessionContext> context = SessionContextHolder.getInstance().getContext();

        return String.valueOf(context.map(sessionContext -> sessionContext.get("domain")).orElseGet(() -> ""));
    }

    /**
     * <p>
     * Encodes the prompt value so that multiple space separated values are sent as %20
     * </p>
     *
     * @param prompt Represents the configured prompt value
     * @return the url encoded prompt
     */
    default String encodePrompt(final String prompt) {
        return URLEncoder.encode(prompt, StandardCharsets.UTF_8).replace("+", "%20");
    }
}