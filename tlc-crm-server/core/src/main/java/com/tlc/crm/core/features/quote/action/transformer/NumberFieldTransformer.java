package com.tlc.crm.core.features.quote.action.transformer;

import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;

import java.util.Map;

/**
 * <p>
 * Manages quote number field related transformer actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Transformer(name = "quote_number", type = Transformer.Type.FIELD)
public class NumberFieldTransformer implements FieldTransformer {

    private static final String QUOTATION_NUMBER_FORMAT = "QT-%03d";

    public NumberFieldTransformer() {
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organization id.
     * @param fieldName The name of the field to use for the transformation.
     * @param entity    The map of data to transform.
     * @return An Object representing the transformed.
     */
    @Override
    public Object transform(final long orgId, final String fieldName, final Map<String, Object> entity) {
        return String.format(QUOTATION_NUMBER_FORMAT, (int) entity.get(fieldName));
    }
}
