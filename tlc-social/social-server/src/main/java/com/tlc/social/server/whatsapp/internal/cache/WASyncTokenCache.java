package com.tlc.social.server.whatsapp.internal.cache;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.remote.CacheService;
import com.tlc.cache.service.provider.CacheServiceProvider;

import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.sync.service.oauth.fields.SyncServiceFields;
import com.tlc.sync.service.oauth.internal.AccessTokenCache;
import com.tlc.sync.service.oauth.model.request.TokenReadRequest;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;
import com.tlc.sync.service.oauth.model.request.whatsapp.WATokenReadRequest;
import com.tlc.sync.service.oauth.model.request.whatsapp.WATokenWriteRequest;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Provides the cache service to store and retrieve the Whatsapp access token based on the WhatsApp business account id
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WASyncTokenCache implements AccessTokenCache {

    private final Cache<Long, String> accessTokenCache;

    private WASyncTokenCache() {
        final CacheService cacheService = CacheServiceProvider.get();
        final CacheConfig<Long, String> accessTokenConfig = new CacheConfig<>(SyncServiceFields.WA_ACCESS_TOKEN);

        accessTokenConfig.setTtl((int) TimeUnit.HOURS.toMillis(NumericalConstant.TWO));

        accessTokenCache = cacheService.createOrOpenCache(accessTokenConfig);
    }

    /**
     * Holds the instance of {@link WASyncTokenCache}
     */
    private static final class InstanceHolder {
        private static final WASyncTokenCache INSTANCE = new WASyncTokenCache();
    }

    /**
     * <p>
     * Gets the {@link WASyncTokenCache} Instance
     * </p>
     *
     * @return {@link WASyncTokenCache}
     */
    public static WASyncTokenCache getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     */
    @Override
    public void putAccessToken(final TokenWriteRequest tokenWriteRequest) {
        final WATokenWriteRequest waTokenWriteRequest = (WATokenWriteRequest) tokenWriteRequest;

        accessTokenCache.put(waTokenWriteRequest.getWaBusinessAccountId(), waTokenWriteRequest.getToken());
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     * @return the access token
     */
    @Override
    public String getAccessToken(final TokenReadRequest tokenReadRequest) {
        final WATokenReadRequest waTokenReadRequest = (WATokenReadRequest) tokenReadRequest;

        return accessTokenCache.get(waTokenReadRequest.getWaBusinessAccountId());
    }
}