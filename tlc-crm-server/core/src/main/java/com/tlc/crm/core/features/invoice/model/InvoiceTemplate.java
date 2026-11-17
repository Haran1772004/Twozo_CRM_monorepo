package com.tlc.crm.core.features.invoice.model;

import com.tlc.crm.core.features.sales.template.model.AbstractSalesDocumentTemplate;

/**
 * <p>
 * Represents an invoice-specific template.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class InvoiceTemplate extends AbstractSalesDocumentTemplate {

    /**
     * {@inheritDoc}
     *
     * @return {@link Type#INVOICE}.
     */
    @Override
    public Type getType() {
        return Type.INVOICE;
    }
}