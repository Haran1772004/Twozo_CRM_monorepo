package com.tlc.crm.core.notification.email.alert.processor;

import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.core.notification.email.alert.processor.impl.CompletionAlertProcessor;
import com.tlc.crm.core.notification.email.alert.processor.impl.MentionAlertProcessor;

import java.util.Collection;

/**
 * <p>
 * Processor which process email alerts.
 * </p>
 *
 * @author Mohammed Asif J.
 * @version 1.0
 */
public interface EmailAlertProcessor {

    /**
     * <p>
     * Provides the instance of email alert processor based on the given template.
     * </p>
     *
     * @param template Represents the template of the email alert
     * @return The instance of {@link EmailAlertProcessor} based on the given template
     */
    static EmailAlertProcessor get(final EmailNotificationTemplate template) {
        return switch (template) {
            case CONTACT_NOTE_MENTION, CONTACT_NOTE_COMMENT_MENTION,
                 DEAL_NOTE_MENTION, DEAL_NOTE_COMMENT_MENTION,
                 COMPANY_NOTE_MENTION, COMPANY_NOTE_COMMENT_MENTION,
                 ACTIVITY_NOTE_MENTION -> MentionAlertProcessor.getInstance();
            case ACTIVITY_COMPLETED -> CompletionAlertProcessor.getInstance();
            default -> null;
        };
    }

    /**
     * <p>
     * Processes the email alert
     * </p>
     *
     * @param orgId    Represents the organization ID
     * @param userId   Represents the user ID
     * @param template Represents the template of the email alert
     * @param ids      Represents the collection of model IDs which triggered the email alert
     */
    void process(final long orgId, final long userId, final EmailNotificationTemplate template, final Collection<Long> ids);
}