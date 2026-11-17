package com.tlc.crm.core.api.deal.model;

import com.tlc.l10n.temporal.model.Date;

import java.util.Objects;

/***
 * A model represents the email summary details
 *
 * @author Kishore J
 * @version 1.0
 */
public class EmailSummary {

    private long emailMessageCount;
    private Date lastEmailReceived;
    private Date lastEmailSent;

    public long getEmailMessageCount() {
        return emailMessageCount;
    }

    public void setEmailMessageCount(final Long emailMessageCount) {
        this.emailMessageCount = Objects.requireNonNullElseGet(emailMessageCount, () -> 0L);
    }

    public Date getLastEmailReceived() {
        return lastEmailReceived;
    }

    public void setLastEmailReceived(final Date lastEmailReceived) {
        this.lastEmailReceived = lastEmailReceived;
    }

    public Date getLastEmailSent() {
        return lastEmailSent;
    }

    public void setLastEmailSent(final Date lastEmailSent) {
        this.lastEmailSent = lastEmailSent;
    }
}
