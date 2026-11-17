package com.tlc.crm.core.features.sales.template.model;

import com.tlc.commons.template.model.TemplateModel;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Represents the common template data model used for sales documents such as
 * quotations and invoices.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public abstract class AbstractSalesDocumentTemplateData implements TemplateModel {

    private final Map<String, Object> data;

    /**
     * <p>
     * Creates an empty sales document template data holder.
     * </p>
     */
    protected AbstractSalesDocumentTemplateData() {
        this.data = new HashMap<>();
    }

    /**
     * <p>
     * Adds a value to the template data using the provided key.
     * </p>
     *
     * @param key   Key used to access the value in the template.
     * @param value Value to be stored in the template data.
     * @return Current {@link AbstractSalesDocumentTemplateData} instance for method chaining.
     */
    protected AbstractSalesDocumentTemplateData addValue(final String key, final Object value) {
        this.data.put(key, value);

        return this;
    }

    /**
     * <p>
     * Retrieves a value from the template data using the given key.
     * </p>
     *
     * @param key Key associated with the required value.
     * @return Stored value for the given key, or {@code null} when unavailable.
     */
    public final Object get(final String key) {
        return this.data.get(key);
    }
}