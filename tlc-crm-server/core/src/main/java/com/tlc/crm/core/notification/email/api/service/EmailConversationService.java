package com.tlc.crm.core.notification.email.api.service;

import com.tlc.crm.core.notification.email.api.service.impl.EmailConversationServiceImpl;
import com.tlc.email.common.model.CategorizedMailContent;

import java.util.Optional;

/**
 * <p>
 * Provides an abstraction for operations related to email conversations.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface EmailConversationService {

    /**
     * <p>
     * Fetches the singleton instance of {@link EmailConversationService}.
     * </p>
     *
     * @return The singleton instance of {@link EmailConversationService}.
     */
    static EmailConversationService getInstance() {
        return EmailConversationServiceImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the mail content for a given conversation, if available.
     * </p>
     *
     * @param orgId The organization ID under which the conversation exists.
     * @param id    The unique identifier of the email conversation.
     * @return An {@link Optional} containing the {@link CategorizedMailContent}.
     */
    Optional<CategorizedMailContent> getMailContent(final Long orgId, final Long id);

    /**
     * <p>
     * Checks if a conversation exists for the given identifiers.
     * </p>
     *
     * @param orgId  The organization ID under which the conversation is checked.
     * @param userId The user ID associated with the conversation.
     * @param id     The unique identifier of the email conversation.
     * @return {@code true} if the conversation exists, {@code false} otherwise.
     */
    boolean exists(final Long orgId, final Long userId, final Long id);

    /**
     * <p>
     * Retrieves the textual content of an email conversation by its ID.
     * </p>
     *
     * @param orgId The organization ID under which the conversation exists.
     * @param id    The unique identifier of the email conversation. Cannot be {@code null}.
     * @return The conversation content as a {@link String}, or an empty string if no content is found.
     */
    String getContent(final Long orgId, final Long id);
}