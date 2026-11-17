package com.tlc.social.server.whatsapp.internal.resolver;

import com.tlc.social.server.whatsapp.internal.cache.WASyncTokenCache;
import com.tlc.social.server.whatsapp.manager.WABusinessManager;
import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.sync.service.oauth.model.request.TokenReadRequest;
import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;
import com.tlc.sync.service.oauth.model.request.whatsapp.WATokenReadRequest;
import com.tlc.sync.service.oauth.model.request.whatsapp.WATokenWriteRequest;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides services to resolve the Whatsapp sync tokens
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WASyncTokenResolverImpl implements WASyncTokenResolver {

    private final WASyncTokenCache waSyncTokenCache;
    private final WABusinessManager waBusinessManager;
    private final ReentrantReadWriteLock lock;

    public WASyncTokenResolverImpl() {
        waSyncTokenCache = WASyncTokenCache.getInstance();
        waBusinessManager = WABusinessManager.getInstance();
        lock = new ReentrantReadWriteLock();
    }

    /**
     * Holds the instance of {@link WASyncTokenResolverImpl}
     */
    private static final class InstanceHolder {
        private static final WASyncTokenResolverImpl INSTANCE = new WASyncTokenResolverImpl();
    }

    /**
     * <p>
     * Gets the {@link WASyncTokenResolverImpl} instance
     * </p>
     *
     * @return {@link WASyncTokenResolverImpl} instance
     */
    public static WASyncTokenResolverImpl getInstance() {
        return InstanceHolder.INSTANCE;
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
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final String accessToken;

        readLock.lock();

        try {
            accessToken = waSyncTokenCache.getAccessToken(waTokenReadRequest);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(accessToken)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final Optional<String> optionalAccessToken = waBusinessManager.getWABusinessToken(waTokenReadRequest);

                if (optionalAccessToken.isPresent()) {
                    final String newAccessToken = optionalAccessToken.get();
                    final WATokenWriteRequest waTokenWriteRequest = SyncTokenRequestInstanceProvider.getWATokenWriteRequest(waTokenReadRequest.getWaBusinessAccountId(), newAccessToken, TokenRequest.TokenType.ACCESS);

                    waSyncTokenCache.putAccessToken(waTokenWriteRequest);

                    return newAccessToken;
                }
            } finally {
                writeLock.unlock();
            }
        }

        return accessToken;
    }

    /**
     * {@inheritDoc}
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     */
    @Override
    public void putAccessToken(final TokenWriteRequest tokenWriteRequest) {
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {
            waSyncTokenCache.putAccessToken(tokenWriteRequest);
        } finally {
            writeLock.unlock();
        }
    }
}
