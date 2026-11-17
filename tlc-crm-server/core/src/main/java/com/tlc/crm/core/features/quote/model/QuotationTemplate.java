package com.tlc.crm.core.features.quote.model;

import com.tlc.crm.core.features.sales.template.model.AbstractSalesDocumentTemplate;

/**
 * <p>
 * Represents a quotation-specific template.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class QuotationTemplate extends AbstractSalesDocumentTemplate {

    /**
     * {@inheritDoc}
     *
     * @return {@link Type#QUOTATION}.
     */
    @Override
    public Type getType() {
        return Type.QUOTATION;
    }
}