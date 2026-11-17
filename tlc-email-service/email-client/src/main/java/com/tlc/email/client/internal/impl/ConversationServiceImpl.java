package com.tlc.email.client.internal.impl;

import com.tlc.email.client.service.ConversationService;

import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.CategorizedMailContent;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.type.Email;

import com.tlc.email.server.internal.manager.EmailManager;
import com.tlc.email.server.internal.manager.ReferenceManager;
import com.tlc.email.server.internal.manager.ThreadManagerImpl;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Provides the functionalities of conversation service.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see ConversationService
 */
public class ConversationServiceImpl implements ConversationService {

    private static final String EMPTY_STRING = "";

    private final EmailManager emailManager;
    private final ThreadManagerImpl threadManager;
    private final ReferenceManager referenceManager;

    private ConversationServiceImpl() {
        this.emailManager = EmailManager.getInstance();
        this.threadManager = ThreadManagerImpl.getInstance();
        this.referenceManager = ReferenceManager.getInstance();
    }

    /**
     * <p>
     * Holds the single instance of conversation service.
     * </p>
     */
    private static class Instance {
        private static final ConversationServiceImpl INSTANCE = new ConversationServiceImpl();
    }

    public static ConversationService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param messageIds a collection of unique conversation identifiers
     * @return the message ids
     */
    @Override
    public Map<Long, String> getMessages(final Collection<Long> messageIds) {
        return emailManager.getMessages(messageIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param messageId Represents the message id
     * @return the message pk value
     */
    @Override
    public Optional<Long> fetchMessagePkValue(final String messageId) {
        return emailManager.fetchMessagePkValue(messageId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param conversationId Represents the conversation id
     * @return {@link Email}
     */
    @Override
    public Optional<Email> getConversation(final Long conversationId) {
        return emailManager.get(conversationId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param threadId Represents the thread id
     * @return {@link Thread}
     */
    @Override
    public Optional<Thread> getThread(final Long threadId) {
        return threadManager.get(threadId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return {@link EmailAddress}
     */
    @Override
    public Optional<EmailAddress> getSyncEmailAddress(final String emailAddress) {
        return emailManager.getSyncEmailAddress(emailAddress);
    }

    /**
     * <p>
     * Gets the {@link EmailAddress}.
     * </p>
     *
     * @param emailId Represents the email address id
     * @return {@link EmailAddress}
     */
    @Override
    public Optional<EmailAddress> getEmailAddress(final Long emailId) {
        return emailManager.getEmailAddress(emailId);
    }

    /**
     * {@inheritDoc}
     *
     * @param emailMetaData Represents the {@link EmailMetaData}
     * @param messageId     Represents the message id
     */
    @Override
    public void buildMetaData(final EmailMetaData emailMetaData, final Long messageId) {
        emailManager.populateEmailMetaData(emailMetaData, messageId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return {@link EmailAddress}
     */
    @Override
    public Optional<EmailAddress> getEmailAddress(final String emailAddress) {
        return emailManager.getEmailAddress(emailAddress);
    }

    /**
     * {@inheritDoc}
     *
     * @param messageId Represents the message id
     * @return the optional {@link EmailMetaData}
     */
    @Override
    public Optional<EmailMetaData> getMetaData(final Long messageId) {
        return emailManager.getMetaData(messageId);
    }

    /**
     * {@inheritDoc}
     *
     * @param conversationId Represents the conversation id
     * @return {@link CategorizedMailContent}
     */
    @Override
    public Optional<CategorizedMailContent> getCategorizedMailContent(final Long conversationId) {
        return emailManager.getCategorizedMailContent(conversationId);
    }

    /**
     * {@inheritDoc}
     *
     * @param childMessageId Represents teh child message ID
     * @return parent message ID
     */
    @Override
    public Optional<Long> getParentMessageId(final Long childMessageId) {
        return referenceManager.getParentMessageId(childMessageId);
    }

    /**
     * Retrieves the content of a conversation by its ID.
     *
     * @param conversationId The ID of the conversation whose content is to be retrieved.
     * @return The conversation content as a {@link String}.
     */
    @Override
    public String getContent(Long conversationId) {
        return this.getCategorizedMailContent(conversationId).map(CategorizedMailContent::content).orElseGet(() -> EMPTY_STRING);
    }

    /**
     * <p>
     * Retrieves the synced email address mapped to the given conversation ID, if available.
     * </p>
     *
     * @param id                            The unique identifier of the conversation.
     * @param includeCurrentMailBoxCriteria Represent whether to include the current mailBox criteria or not.
     * @return An {@link Optional} containing the mapped email address if present, otherwise an empty Optional.
     */
    @Override
    public Optional<String> getConversationMailBoxAddress(final Long id, final boolean includeCurrentMailBoxCriteria) {
        if (Objects.isNull(id)) {
            return Optional.empty();
        }
        return emailManager.getConversationMailBoxAddress(id, includeCurrentMailBoxCriteria);
    }
}
