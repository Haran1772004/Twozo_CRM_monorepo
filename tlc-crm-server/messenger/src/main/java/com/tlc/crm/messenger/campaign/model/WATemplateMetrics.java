package com.tlc.crm.messenger.campaign.model;

/**
 * <p>
 * Represents aggregated WhatsApp template metrics.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WATemplateMetrics {

    private final int sent;
    private final int deliveredPercentage;
    private final int readPercentage;

    public WATemplateMetrics(final int sent, final int deliveredPercentage, final int readPercentage) {
        this.sent = sent;
        this.deliveredPercentage = deliveredPercentage;
        this.readPercentage = readPercentage;
    }

    public int getSent() {
        return sent;
    }

    public int getDeliveredPercentage() {
        return deliveredPercentage;
    }

    public int getReadPercentage() {
        return readPercentage;
    }
}