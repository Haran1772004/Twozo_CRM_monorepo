package com.tlc.email.client.service;

import com.tlc.email.client.internal.impl.ConversationServiceImpl;

import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.CategorizedMailContent;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.type.Email;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Provides the services of getting the conversation related services.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface ConversationService {

    static ConversationService get() {
        return ConversationServiceImpl.getInstance();
    }

    /**
     * Retrieves the message IDs for the specified {@link Collection} of conversation IDs.
     *
     * @param messageIds a collection of unique conversation identifiers
     * @return a collection of message IDs associated with the given conversation IDs
     */
    Map<Long, String> getMessages(final Collection<Long> messageIds);

    /**
     * Fetches the external message pk value.
     *
     * @param messageId Represents the message id
     * @return the message pk value
     */
    Optional<Long> fetchMessagePkValue(final String messageId);

    /**
     * <p>
     * Gets the {@link Email} for the given conversation id.
     * </p>
     *
     * @param conversationId Represents the conversation id
     * @return {@link Email}
     */
    Optional<Email> getConversation(final Long conversationId);

    /**
     * <p>
     * Gets the {@link Thread} for the given thread id.
     * </p>
     *
     * @param threadId Represents the thread id
     * @return {@link Thread}
     */
    Optional<Thread> getThread(final Long threadId);

    /**
     * <p>
     * Gets the sync {@link EmailAddress} for an given address.
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return {@link EmailAddress}
     */
    Optional<EmailAddress> getSyncEmailAddress(final String emailAddress);

    /**
     * <p>
     * Gets the {@link EmailAddress} for an given address id.
     * </p>
     *
     * @param emailId Represents the email address id
     * @return {@link EmailAddress}
     */
    Optional<EmailAddress> getEmailAddress(final Long emailId);

    /**
     * <p>
     * Gets the {@link EmailAddress} for an given address.
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return {@link EmailAddress}
     */
    Optional<EmailAddress> getEmailAddress(final String emailAddress);

    /**
     * <p>
     * Builds the {@link EmailMetaData} by using the message id
     * </p>
     *
     * @param emailMetaData Represents the {@link EmailMetaData}
     * @param messageId     Represents the message id
     */
    void buildMetaData(final EmailMetaData emailMetaData, final Long messageId);

    /**
     * <p>
     * Gets the optional {@link EmailMetaData} from the provided message id
     * </p>
     *
     * @param messageId Represents the message id
     * @return the optional {@link EmailMetaData}
     */
    Optional<EmailMetaData> getMetaData(final Long messageId);

    /**
     * <p>
     * Gets the subject and content from optional {@link CategorizedMailContent} for the provided conversation id
     * </p>
     *
     * @param conversationId Represents the conversation id
     * @return the optional {@link CategorizedMailContent}
     */
    Optional<CategorizedMailContent> getCategorizedMailContent(final Long conversationId);

    /**
     * Gets the parent message ID by given child message ID.
     *
     * @param childMessageId Represents teh child message ID
     * @return parent message ID
     */
    Optional<Long> getParentMessageId(final Long childMessageId);

    /**
     * Retrieves the content of a conversation by its ID.
     *
     * @param conversationId The ID of the conversation whose content is to be retrieved.
     * @return The conversation content as a {@link String}.
     */
    String getContent(final Long conversationId);

    /**
     * <p>
     * Retrieves the synced email address mapped to the given conversation ID, if available.
     * </p>
     *
     * @param id                            The unique identifier of the conversation.
     * @param includeCurrentMailBoxCriteria Represent whether to include the current mailBox criteria or not.
     * @return An {@link Optional} containing the mapped email address if present, otherwise an empty Optional.
     */
    Optional<String> getConversationMailBoxAddress(final Long id, final boolean includeCurrentMailBoxCriteria);
}
