package com.tlc.crm.plan.and.billing.api.models;

/**
 * Represents a summary of invoices for an organization, including the last generated invoice and the next estimated invoice.
 *
 * @author Sathish
 * @version 1.0
 */
public class InvoiceSummary {

    private Invoice lastInvoice;
    private Invoice nextEstimatedInvoice;

    public Invoice getLastInvoice() {
        return lastInvoice;
    }

    public void setLastInvoice(final Invoice lastInvoice) {
        this.lastInvoice = lastInvoice;
    }

    public Invoice getNextEstimatedInvoice() {
        return nextEstimatedInvoice;
    }

    public void setNextEstimatedInvoice(final Invoice nextEstimatedInvoice) {
        this.nextEstimatedInvoice = nextEstimatedInvoice;
    }
}
