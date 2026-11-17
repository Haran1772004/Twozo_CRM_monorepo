package com.tlc.crm.messenger.internal.service.impl;

import com.tlc.crm.messenger.api.conversation.model.ConversationActivityTemplate;
import com.tlc.crm.messenger.internal.resolver.ConversationActivityTemplateResolver;
import com.tlc.crm.messenger.internal.service.ConversationActivityTemplateService;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;

/**
 * <p>
 * Converts the {@link DateTime} field in {@link TlcModel}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class ConversationActivityTemplateServiceImpl implements ConversationActivityTemplateService {

    private final ConversationActivityTemplateResolver conversationActivityTemplateResolver;

    private static class Instance {
        private static final ConversationActivityTemplateServiceImpl INSTANCE = new ConversationActivityTemplateServiceImpl();
    }

    private ConversationActivityTemplateServiceImpl() {
        conversationActivityTemplateResolver = ConversationActivityTemplateResolver.getInstance();
    }

    /**
     * <p>
     * Gets the {@link ConversationActivityTemplateServiceImpl}.
     * </p>
     *
     * @return {@link ConversationActivityTemplateServiceImpl}.
     */
    public static ConversationActivityTemplateServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    @Override
    public ConversationActivityTemplate get(final Long typeId) {
        return conversationActivityTemplateResolver.resolve(typeId);
    }
}