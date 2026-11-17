package com.tlc.crm.core.features.invoice.model;

import com.tlc.crm.core.features.sales.template.model.AbstractSalesDocumentTemplateData;

/**
 * <p>
 * Represents the template data model used for generating invoice templates.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class InvoiceTemplateData extends AbstractSalesDocumentTemplateData {

    /**
     * <p>
     * Creates an empty invoice template data holder.
     * </p>
     */
    public InvoiceTemplateData() {
        super();
    }

    /**
     * <p>
     * Adds a value to the invoice template data using the provided key.
     * </p>
     *
     * @param key   Key used to access the value in the template.
     * @param value Value to be stored in the template data.
     * @return Current {@link InvoiceTemplateData} instance for method chaining.
     */
    @Override
    public InvoiceTemplateData addValue(final String key, final Object value) {
        super.addValue(key, value);

        return this;
    }
}