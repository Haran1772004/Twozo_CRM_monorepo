package com.tlc.social.server.instagram.internal.resolver;

import com.tlc.commons.concurrent.LockStore;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.server.instagram.internal.cache.InstagramPageTokenCache;
import com.tlc.social.server.instagram.manager.InstagramPageManager;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Acts as a bridge between Cache and DB to fetch/store Instagram's page token.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class InstagramPageTokenResolverImpl implements InstagramPageTokenResolver {

    private static final byte DEFAULT_STRIPES_COUNT = 10;

    private final InstagramPageTokenCache instagramPageTokenCache;
    private final InstagramPageManager instagramPageManager;
    private final LockStore<Long> lockStore;

    private InstagramPageTokenResolverImpl() {
        final Configurations configurations = ConfHolder.getInstance().getProperties(FBFields.SOCIAL).getNestedConfigs(FBFields.INSTAGRAM);
        instagramPageTokenCache = InstagramPageTokenCache.getInstance();
        instagramPageManager = InstagramPageManager.getInstance();
        lockStore = new LockStore<>(configurations.optInt(FBFields.PAGE_TOKEN_LOCK_COUNT, DEFAULT_STRIPES_COUNT));
    }

    private static final class InstanceHolder {
        private static final InstagramPageTokenResolverImpl PAGE_TOKEN_RESOLVER = new InstagramPageTokenResolverImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link InstagramPageTokenResolverImpl}
     * </p>
     *
     * @return the instance of {@link InstagramPageTokenResolverImpl}
     */
    public static InstagramPageTokenResolverImpl getInstance() {
        return InstanceHolder.PAGE_TOKEN_RESOLVER;
    }


    /**
     * {@inheritDoc}
     *
     * @param pageId          Represents the page id
     * @param pageAccessToken Represents the page access token
     */
    @Override
    public void put(final Long pageId, final String pageAccessToken) {
        final ReentrantReadWriteLock lock = lockStore.getLock(pageId);
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {
            instagramPageTokenCache.put(pageId, pageAccessToken);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param pageId Represents the page id
     * @return the page access token
     */
    @Override
    public String get(final Long pageId) {
        final ReentrantReadWriteLock lock = lockStore.getLock(pageId);
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final String token;

        readLock.lock();

        try {
            token = instagramPageTokenCache.get(pageId);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(token)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();
            try {
                final Optional<String> optionalToken = instagramPageManager.getPageAccessToken(pageId);

                if (optionalToken.isPresent()) {
                    final String pageAccessToken = optionalToken.get();

                    instagramPageTokenCache.put(pageId, pageAccessToken);

                    return pageAccessToken;
                }
            } finally {
                writeLock.unlock();
            }
        }

        return token;
    }
}
