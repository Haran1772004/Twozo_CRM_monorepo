package com.tlc.notification.internal.contact.resolver;

import com.tlc.commons.concurrent.LockStore;
import com.tlc.notification.internal.contact.token.ContactTokenCache;
import com.tlc.notification.model.TokenType;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Provides the services to resolve the tokens used for contact synchronization
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
class ContactTokenResolverImpl implements ContactTokenResolver {

    private static final byte STRIPES_COUNT = 10;

    private final ContactTokenCache contactTokenCache;
    private final LockStore<String> lockStore;

    private ContactTokenResolverImpl() {
        this.contactTokenCache = ContactTokenCache.getInstance();
        this.lockStore = new LockStore<>(STRIPES_COUNT);
    }

    private static class Instance {
        private static final ContactTokenResolverImpl INSTANCE = new ContactTokenResolverImpl();
    }

    /**
     * <p>
     * Gets the {@link ContactTokenResolverImpl} instance
     * </p>
     *
     * @return {@link ContactTokenResolverImpl} instance
     */
    public static ContactTokenResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddress Represents the email address
     * @return the sync token
     */
    @Override
    public String getSyncToken(final String emailAddress) {
        final ReentrantReadWriteLock.ReadLock readLock = lockStore.getLock(emailAddress).readLock();
        final String syncToken;

        readLock.lock();

        try {
            syncToken = contactTokenCache.getSyncToken(emailAddress);
        } finally {
            readLock.unlock();
        }

        return syncToken;
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddress Represents the email address
     * @param token        Represents the sync token
     */
    @Override
    public void putSyncToken(final String emailAddress, final String token) {
        final ReentrantReadWriteLock.WriteLock writeLock = lockStore.getLock(emailAddress).writeLock();

        writeLock.lock();

        try {
            contactTokenCache.putSyncToken(emailAddress, token);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearAllSyncTokens() {
        contactTokenCache.clearSyncToken();
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddress Represents the email address
     * @return the page token
     */
    @Override
    public String getPageToken(final String emailAddress) {
        final ReentrantReadWriteLock.ReadLock readLock = lockStore.getLock(emailAddress).readLock();
        final String pageToken;

        readLock.lock();

        try {
            pageToken = contactTokenCache.getPageToken(emailAddress);
        } finally {
            readLock.unlock();
        }

        return pageToken;
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddress Represents the email address
     * @param token        Represents the page token
     */
    @Override
    public void putPageToken(final String emailAddress, final String token) {
        final ReentrantReadWriteLock.WriteLock writeLock = lockStore.getLock(emailAddress).writeLock();

        writeLock.lock();

        try {
            contactTokenCache.putPageToken(emailAddress, token);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddress Represents the email address
     * @param tokenName    Represents the token name
     */
    @Override
    public void clearToken(final String emailAddress, final TokenType tokenName) {
        contactTokenCache.clearToken(emailAddress, tokenName);
    }
}
