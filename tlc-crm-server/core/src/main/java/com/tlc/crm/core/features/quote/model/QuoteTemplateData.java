package com.tlc.crm.core.features.quote.model;

import com.tlc.crm.core.features.sales.template.model.AbstractSalesDocumentTemplateData;

/**
 * <p>
 * Represents the template data model used for generating quotation templates.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class QuoteTemplateData extends AbstractSalesDocumentTemplateData {

    /**
     * Creates an empty quotation template data holder.
     */
    public QuoteTemplateData() {
        super();
    }

    /**
     * Adds a value to the template data using the provided key.
     *
     * <p>If the key already exists, the existing value will be replaced with
     * the new value.
     *
     * @param key   Key used to access the value in the template.
     * @param value Value to be stored in the template data.
     * @return Current {@link QuoteTemplateData} instance for method chaining.
     */
    @Override
    public QuoteTemplateData addValue(final String key, final Object value) {
        super.addValue(key, value);

        return this;
    }
}