package com.tlc.crm.core.features.invoice.model;

import com.tlc.crm.core.features.sales.template.model.LineItem;

/**
 * <p>
 * Represents a single invoice line item, extending {@link LineItem} with a reference back
 * to its parent {@link Invoice} via {@link #invoiceId}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class InvoiceLineItem extends LineItem {

    private Long invoiceId;

    public InvoiceLineItem() {
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(final Long invoiceId) {
        this.invoiceId = invoiceId;
    }
}