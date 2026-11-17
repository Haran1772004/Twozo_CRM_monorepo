package com.tlc.email.server.service;

import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.InboundEmail;
import com.tlc.email.common.model.type.OutboundEmail;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Provides utility methods to map and convert {@link Email} objects between inbound and outbound types.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailMapper {

    private EmailMapper() {
    }

    /**
     * <p>
     * Converts the given {@link Email} to an {@link InboundEmail}.
     * </p>
     *
     * @param existingEmail the original email to convert
     * @return a new {@link InboundEmail} with properties copied from {@code existingEmail}
     */
    public static Email toInbound(final Email existingEmail) {
        final Email email = new InboundEmail();

        loadEmail(existingEmail, email);
        email.setMessageCategory(Email.MessageCategory.INBOUND);

        return email;
    }

    /**
     * <p>
     * Converts the given {@link Email} to an {@link OutboundEmail}.
     * </p>
     *
     * @param existingEmail the original email to convert
     * @return a new {@link OutboundEmail} with properties copied from {@code existingEmail}
     * and an appropriately determined outbound type
     */
    public static Email toOutbound(final Email existingEmail) {
        final Email email = new OutboundEmail();

        loadEmail(existingEmail, email);
        email.setMessageCategory(Email.MessageCategory.OUTBOUND);
        setOutboundType(email, email.getMetaData());

        return email;
    }

    /**
     * <p>
     * Copies common properties from the source email to the target email.
     * </p>
     *
     * @param existingEmail the source email from which properties are copied
     * @param email         the target email that receives the copied properties
     */
    private static void loadEmail(final Email existingEmail, final Email email) {
        email.setSubject(existingEmail.getSubject());
        email.setContent(existingEmail.getContent());
        email.setRecipients(existingEmail.getRecipients());
        email.setSender(existingEmail.getSender());
        email.setAttachments(existingEmail.getAttachments());
        email.setMetaData(existingEmail.getMetaData());
    }

    /**
     * <p>
     * Determines and sets the {@link OutboundEmail.OutboundType} for an {@link OutboundEmail}.
     * </p>
     *
     * @param email    The email to check and cast to {@link OutboundEmail} if applicable
     * @param metaData The {@link EmailMetaData} used to check for reply context
     */
    private static void setOutboundType(final Email email, final EmailMetaData metaData) {
        final Email.MessageCategory messageCategory = email.getMessageCategory();

        if (Objects.nonNull(messageCategory) && messageCategory.isSendCategory()) {
            final OutboundEmail outboundEmail = (OutboundEmail) email;
            final String inReplyTo = metaData.getInReplyTo();

            if (Objects.nonNull(inReplyTo) && !inReplyTo.isEmpty()) {
                final EmailRecipients emailRecipients = outboundEmail.getEmailRecipients();

                if (Objects.nonNull(emailRecipients)) {
                    final Collection<EmailAddress> toRecipients = emailRecipients.toRecipients();
                    final Collection<EmailAddress> ccRecipients = emailRecipients.ccRecipients();
                    final int recipientCount = (Objects.nonNull(toRecipients) ? toRecipients.size() : 0) +
                            (Objects.nonNull(ccRecipients) ? ccRecipients.size() : 0);

                    if (recipientCount > 1) {
                        outboundEmail.setOutboundType(OutboundEmail.OutboundType.REPLY_ALL);
                    } else {
                        outboundEmail.setOutboundType(OutboundEmail.OutboundType.REPLY);
                    }
                }
            } else {
                outboundEmail.setOutboundType(OutboundEmail.OutboundType.SEND);
            }
        }
    }
}