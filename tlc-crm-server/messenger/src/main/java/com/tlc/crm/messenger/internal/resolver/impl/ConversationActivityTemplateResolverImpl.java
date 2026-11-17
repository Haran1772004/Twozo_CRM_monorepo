package com.tlc.crm.messenger.internal.resolver.impl;

import com.tlc.commons.concurrent.LockStore;
import com.tlc.crm.messenger.api.conversation.manager.ConversationManager;
import com.tlc.crm.messenger.api.conversation.model.ConversationActivityTemplate;
import com.tlc.crm.messenger.internal.cache.internal.ConversationActivityTemplateCacheImpl;
import com.tlc.crm.messenger.internal.resolver.ConversationActivityTemplateResolver;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Offers the access to meta data of the conversation's activity template.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public class ConversationActivityTemplateResolverImpl implements ConversationActivityTemplateResolver {

    private static final byte STRIPES_COUNT = 3;

    private final ConversationActivityTemplateCacheImpl activityTemplateCache;
    private final LockStore<Long> lockStore;
    private final ConversationManager conversationManager;

    private static class Instance {
        private static final ConversationActivityTemplateResolverImpl INSTANCE = new ConversationActivityTemplateResolverImpl();
    }

    /**
     * <p>
     * Gets the {@link PlaceHolderValueResolverImpl}.
     * </p>
     *
     * @return @link PlaceHolderValueResolverImpl}.
     */
    public static ConversationActivityTemplateResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    private ConversationActivityTemplateResolverImpl() {
        activityTemplateCache = ConversationActivityTemplateCacheImpl.getInstance();
        lockStore = new LockStore<>(STRIPES_COUNT);
        conversationManager = ConversationManager.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConversationActivityTemplate resolve(final Long typeId) {
        final ReentrantReadWriteLock reentrantReadWriteLock = lockStore.getLock(typeId);
        final ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
        final ConversationActivityTemplate existingTemplate;

        readLock.lock();

        try {
            existingTemplate = activityTemplateCache.get(typeId);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(existingTemplate)) {
            final ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

            writeLock.lock();
            final ConversationActivityTemplate conversationActivityTemplate;

            try {
                conversationActivityTemplate = conversationManager.getActivityTemplate(typeId);

                activityTemplateCache.add(typeId, conversationActivityTemplate);
            } finally {
                writeLock.unlock();
            }
            return conversationActivityTemplate;
        }
        return existingTemplate;
    }
}