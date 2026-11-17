package com.tlc.crm.email.notification.email.api.model;

import com.tlc.crm.email.notification.email.api.model.settings.EmailTrackerSettings;
import com.tlc.crm.email.notification.email.api.model.settings.EmailLinkerSettings;

import com.tlc.notification.model.template.PlaceHolder;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.client.template.MailTemplate;

import java.util.Collection;

/**
 * <p>
 * Represents the {@link TwozoEmail} which wraps the twozo related settings services.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class TwozoEmail extends Email {

    private EmailLinkerSettings emailLinkerSettings;
    private EmailTrackerSettings emailTrackerSettings;
    private boolean hasUnsubscribeLink;
    private MailTemplate mailTemplate;

    public TwozoEmail() {
        this.mailTemplate = getMailTemplate();
    }

    public EmailLinkerSettings getEmailLinkerSettings() {
        return emailLinkerSettings;
    }

    public void setEmailLinkerSettings(final EmailLinkerSettings emailLinkerSettings) {
        this.emailLinkerSettings = emailLinkerSettings;
    }

    public EmailTrackerSettings getEmailTrackerSettings() {
        return emailTrackerSettings;
    }

    public void setEmailTrackerSettings(final EmailTrackerSettings emailTrackerSettings) {
        this.emailTrackerSettings = emailTrackerSettings;
    }

    public boolean hasUnsubscribeLink() {
        return hasUnsubscribeLink;
    }

    public void setHasUnsubscribeLink(final boolean hasUnsubscribeLink) {
        this.hasUnsubscribeLink = hasUnsubscribeLink;
    }

    public boolean isSubjectHasPlaceHolders() {
        return mailTemplate.isSubjectHasPlaceHolders();
    }

    public boolean isContentHasPlaceHolders() {
        return mailTemplate.isContentHasPlaceHolders();
    }

    public boolean hasPlaceHolders() {
        return mailTemplate.hasPlaceHolders();
    }

    public void setPlaceHolders(final Collection<PlaceHolder> placeHolders) {
        mailTemplate.setPlaceHolders(placeHolders);
    }

    public Collection<PlaceHolder> placeHolders() {
        return mailTemplate.placeHolders();
    }

    private MailTemplate getMailTemplate() {

        if (mailTemplate == null) {
            mailTemplate = new MailTemplate();
        }

        return mailTemplate;
    }

    public void setMailTemplate(final MailTemplate mailTemplate) {
        this.mailTemplate = mailTemplate;
    }
}
