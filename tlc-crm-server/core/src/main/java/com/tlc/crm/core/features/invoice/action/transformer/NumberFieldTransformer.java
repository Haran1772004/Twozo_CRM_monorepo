package com.tlc.crm.core.features.invoice.action.transformer;

import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;

import java.util.Map;

/**
 * <p>
 * Transforms the invoice number field for the invoice list.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@Transformer(name = "invoice_number", type = Transformer.Type.FIELD)
public class NumberFieldTransformer implements FieldTransformer {

    private static final String INVOICE_NUMBER_FORMAT = "INV-%03d";

    /**
     * <p>
     * Returns the invoice number from the entity.
     * </p>
     *
     * @param orgId     Represents the organization ID.
     * @param fieldName Represents the name of the field being transformed.
     * @param entity    Represents the invoice field values.
     * @return The invoice number.
     */
    @Override
    public Object transform(final long orgId, final String fieldName, final Map<String, Object> entity) {
        return String.format(INVOICE_NUMBER_FORMAT, (int) entity.get(fieldName));
    }
}