package com.tlc.crm.messenger.internal.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheConfig;
import com.tlc.cache.CacheManager;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.crm.messenger.api.conversation.model.ConversationActivityTemplate;
import com.tlc.crm.messenger.internal.cache.ConversationActivityTemplateCache;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Stores the meta data of the conversation's activity template.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public class ConversationActivityTemplateCacheImpl implements ConversationActivityTemplateCache {

    private final Cache<Long, ConversationActivityTemplate> templateCache;

    private static class Instance {
        private static final ConversationActivityTemplateCacheImpl INSTANCE = new ConversationActivityTemplateCacheImpl();
    }

    public static ConversationActivityTemplateCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    private ConversationActivityTemplateCacheImpl() {
        final CacheConfig<Long, ConversationActivityTemplate> templateCacheConfig = new CacheConfig<>();

        templateCacheConfig.setExpireAfterAccess(TimeUnit.DAYS.toMillis(1));
        templateCache = CacheManager.getInstance().createCache(templateCacheConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConversationActivityTemplate get(final Long id) {
        return templateCache.get(id);
    }

    /**
     * <p>
     * Adds the {@link ConversationActivityTemplate}.
     * </p>
     *
     * @param typeId   Template type's id.
     * @param conversationActivityTemplate {@link ConversationActivityTemplate}.
     */
    public void add(final Long typeId, final ConversationActivityTemplate conversationActivityTemplate) {
        templateCache.put(typeId, conversationActivityTemplate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        throw ErrorCode.get(ErrorCodes.NOT_SUPPORTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        templateCache.clear();
    }
}
