package com.tlc.crm.email.core.internal.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.api.model.email.EntityDraftEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.email.core.OutboundEmailLinkerService;
import com.tlc.crm.email.core.manager.OutboundEmailLinkerManager;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.type.DraftEmail;
import com.tlc.email.common.model.type.Email;
import com.tlc.sql.common.api.DataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static com.tlc.email.common.model.type.Email.MessageCategory.DRAFT;

/**
 * <p>
 * Provides the outbound mail related services
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class OutboundEmailLinkerServiceImpl implements OutboundEmailLinkerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboundEmailLinkerServiceImpl.class);

    private final OutboundEmailLinkerManager outboundEmailLinkerManager;

    private OutboundEmailLinkerServiceImpl() {
        this.outboundEmailLinkerManager = OutboundEmailLinkerManager.getInstance();
    }

    private static class Instance {
        private static final OutboundEmailLinkerServiceImpl INSTANCE = new OutboundEmailLinkerServiceImpl();
    }

    public static OutboundEmailLinkerServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param thread Represents the thread
     */
    @Override
    public void send(final Long orgId, final EntityThread thread) {
        final EntityEmail composedEmail = thread.getComposedEmail();
        final Long userId = thread.getUserId();

        try {
            if (Objects.nonNull(composedEmail)) {
                final Long conversationId = composedEmail.getConversationId();
                final boolean hasConversation = Objects.nonNull(conversationId);
                final boolean isUpdatableDraft = DRAFT == composedEmail.getMessageCategory()
                        && composedEmail instanceof EntityDraftEmail entityDraftEmail
                        && DraftEmail.DraftAction.CREATE != entityDraftEmail.getDraftAction();
                final Email.MessageCategory messageCategory = outboundEmailLinkerManager.messageCategory(orgId, conversationId);
                final boolean isExistingConversationDraft = Objects.nonNull(messageCategory) && messageCategory.isDraft();

                if (hasConversation && (isUpdatableDraft || isExistingConversationDraft)) {
                    outboundEmailLinkerManager.update(orgId, userId, List.of(thread));
                } else {
                    outboundEmailLinkerManager.create(thread);
                }

            }
        } catch (Exception e) {
            LOGGER.error("Email Send: Failed to load the mail into DB, OrgId:{}, UserId:{}, Message: {}", orgId, userId, e.getMessage(), e);
            throw ErrorCode.get(EmailErrorCodes.DB_LOAD_ACTION_FAILED, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param isCreateEvent         Represent whether it is a create event or not
     * @param thread                Represents the {@link EntityThread}
     * @param existingMailContainer Holds the user defined values of existing mail
     * @param targetContainer       Holds the updated information of email defined
     */
    @Override
    public void loadRetrievedMails(final boolean isCreateEvent, final EntityThread thread, final DataContainer existingMailContainer, final DataContainer targetContainer) {
        outboundEmailLinkerManager.loadRetrievedMails(isCreateEvent, thread, existingMailContainer, targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param thread Represents the {@link Thread}
     */
    @Override
    public void createThread(final EntityThread thread) {
        outboundEmailLinkerManager.create(thread);
    }

    /**
     * {@inheritDoc}
     *
     * @param email Represents the {@link Email}
     * @return {@link Collection} of email recipients
     */
    @Override
    public Collection<String> getEmailRecipients(final Email email) {
        final Collection<String> recipients = new HashSet<>();
        final EmailRecipients emailRecipients = email.getEmailRecipients();
        final Function<Collection<EmailAddress>, Collection<String>> getEmailIds = emailAddresses ->
                Optional.ofNullable(emailAddresses).filter(emails -> !emails.isEmpty()).map(emails -> emails.stream()
                        .map(EmailAddress::getEmailAddress).toList()).orElseGet(Collections::emptyList);

        recipients.addAll(getEmailIds.apply(emailRecipients.toRecipients()));
        recipients.addAll(getEmailIds.apply(emailRecipients.bccRecipients()));
        recipients.addAll(getEmailIds.apply(emailRecipients.ccRecipients()));

        return recipients;
    }
}