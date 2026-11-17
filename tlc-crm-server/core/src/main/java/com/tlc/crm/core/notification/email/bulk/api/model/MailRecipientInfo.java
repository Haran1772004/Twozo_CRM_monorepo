package com.tlc.crm.core.notification.email.bulk.api.model;

import com.tlc.crm.core.notification.email.bulk.api.model.MailInfo.Status;
import com.tlc.crm.core.notification.email.api.model.ContactRecipients;

import com.tlc.email.common.model.address.EmailAddress;

/**
 * Holds the information of {@link ContactRecipients}, including their contact details,
 * personalized status, and email-related information.
 *
 * @param contactId      Represents the contact id
 * @param name           Represents the contact name
 * @param isPersonalized Represents whether the mail is personalized or not
 * @param status         Represents the {@link Status} of mail
 * @param contactEmailId Represents the contact email id
 * @param conversationId Represents the conversation id
 * @param emailAddress   Represents the {@link EmailAddress}
 * @author KowsalyaSP
 * @version 1.0
 */
public record MailRecipientInfo(Long contactId, String name, boolean isPersonalized, Status status,
                                Long contactEmailId, Long conversationId, EmailAddress emailAddress) {
}
