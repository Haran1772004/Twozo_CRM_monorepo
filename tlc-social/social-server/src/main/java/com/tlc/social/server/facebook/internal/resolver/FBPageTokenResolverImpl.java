package com.tlc.social.server.facebook.internal.resolver;

import com.tlc.commons.concurrent.LockStore;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

import com.tlc.social.common.facebook.fields.FBFields;
import com.tlc.social.server.facebook.internal.cache.FBPageTokenCache;
import com.tlc.social.server.facebook.manager.FBPageManager;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A resolver implementation of {@link FBPageTokenResolver} which resolves the facebook page access token
 *
 * @author Maharaja S
 * @version 1.0
 */
public class FBPageTokenResolverImpl implements FBPageTokenResolver {

    private static final byte DEFAULT_STRIPES_COUNT = 10;

    private final FBPageTokenCache fbPageTokenCache;
    private final FBPageManager fbPageManager;
    private final LockStore<Long> lockStore;

    private FBPageTokenResolverImpl() {
        final Configurations configurations = ConfHolder.getInstance().getProperties(FBFields.SOCIAL).getNestedConfigs(FBFields.FACEBOOK);
        fbPageTokenCache = FBPageTokenCache.getInstance();
        fbPageManager = FBPageManager.getInstance();
        lockStore = new LockStore<>(configurations.optInt(FBFields.PAGE_TOKEN_LOCK_COUNT, DEFAULT_STRIPES_COUNT));
    }

    private static final class InstanceHolder {
        private static final FBPageTokenResolverImpl PAGE_TOKEN_RESOLVER = new FBPageTokenResolverImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link FBPageTokenResolverImpl}
     * </p>
     *
     * @return the instance of {@link FBPageTokenResolverImpl}
     */
    public static FBPageTokenResolverImpl getInstance() {
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
            fbPageTokenCache.put(pageId, pageAccessToken);
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
            token = fbPageTokenCache.get(pageId);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(token)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();
            try {
                final Optional<String> optionalToken = fbPageManager.getPageAccessToken(pageId);

                if (optionalToken.isPresent()) {
                    final String pageAccessToken = optionalToken.get();

                    fbPageTokenCache.put(pageId, pageAccessToken);

                    return pageAccessToken;
                }
            } finally {
                writeLock.unlock();
            }
        }

        return token;
    }
}
