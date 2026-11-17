package com.tlc.crm.core.api.insights.models;

/**
 * <p>
 * Represents the email insights data.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class EmailInsights {

    private long emailMessageCount;

    private Long lastEmailSent;
    private Long lastEmailReceived;

    /**
     * <p>
     * Gets the email message count.
     * </p>
     *
     * @return the get email message count value
     */
    public long getEmailMessageCount() {
        return emailMessageCount;
    }

    /**
     * <p>
     * Sets the email message count.
     * </p>
     *
     * @param emailMessageCount the email message count
     */
    public void setEmailMessageCount(final long emailMessageCount) {
        this.emailMessageCount = emailMessageCount;
    }

    /**
     * <p>
     * Gets the last email sent.
     * </p>
     *
     * @return the get last email sent value
     */
    public Long getLastEmailSent() {
        return lastEmailSent;
    }

    /**
     * <p>
     * Sets the last email sent.
     * </p>
     *
     * @param lastEmailSent the last email sent
     */
    public void setLastEmailSent(final Long lastEmailSent) {
        this.lastEmailSent = lastEmailSent;
    }

    /**
     * <p>
     * Gets the last email received.
     * </p>
     *
     * @return the get last email received value
     */
    public Long getLastEmailReceived() {
        return lastEmailReceived;
    }

    /**
     * <p>
     * Sets the last email received.
     * </p>
     *
     * @param lastEmailReceived the last email received
     */
    public void setLastEmailReceived(final Long lastEmailReceived) {
        this.lastEmailReceived = lastEmailReceived;
    }
}