package com.tlc.crm.core.notification.email.api.service.impl;

import com.tlc.commons.util.StringUtil;
import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.service.EmailConversationService;
import com.tlc.crm.core.notification.email.tracking.service.EmailTrackingService;
import com.tlc.email.client.service.ConversationService;
import com.tlc.email.common.model.CategorizedMailContent;
import com.tlc.email.common.model.type.Email;

import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Implementation of {@link EmailConversationService} that provides methods for retrieving
 * and validating email conversation data.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailConversationServiceImpl implements EmailConversationService {

    private final CoreMailManager coreMailManager;
    private final ConversationService conversationService;
    private final EmailTrackingService emailTrackingService;

    private EmailConversationServiceImpl() {
        this.coreMailManager = CoreMailManager.getInstance();
        this.conversationService = ConversationService.get();
        this.emailTrackingService = EmailTrackingService.getInstance();
    }

    private static class Instance {
        private static final EmailConversationService INSTANCE = new EmailConversationServiceImpl();
    }

    public static EmailConversationService getInstance() {
        return EmailConversationServiceImpl.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Retrieves the mail content for a given conversation if it exists.
     * </p>
     */
    @Override
    public Optional<CategorizedMailContent> getMailContent(final Long orgId, final Long id) {
        final Optional<Long> conversationId = coreMailManager.getConversationId(orgId, id);

        if (conversationId.isPresent()) {
            return conversationService.getCategorizedMailContent(conversationId.get());
        }

        return coreMailManager.getCategorizedMailContent(orgId, id);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Validates whether a conversation exists for the specified organization, user, and conversation ID.
     * </p>
     */
    @Override
    public boolean exists(final Long orgId, final Long userId, final Long id) {
        return coreMailManager.isConversationExists(orgId, userId, id, true);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Returns the conversation content as a string.
     * </p>
     */
    @Override
    public String getContent(final Long orgId, final Long id) {
        final Optional<CategorizedMailContent> categorizedMailContent = this.getMailContent(orgId, id);

        if (categorizedMailContent.isPresent()) {
            final CategorizedMailContent mailContent = categorizedMailContent.get();
            final String content = mailContent.content();

            if (content.isBlank()) {
                return StringUtil.EMPTY_STRING;
            }
            return Objects.equals(mailContent.messageCategory(), Email.MessageCategory.INBOUND)
                    ? content : emailTrackingService.removeTrackingDetails(content);
        }

        return StringUtil.EMPTY_STRING;
    }
}