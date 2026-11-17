package com.tlc.crm.entity.action.transformer;

import com.tlc.commons.json.Json;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;

import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Transforms the check in and check out field entries into required forms
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@Transformer(name = "checkInCheckOut", type = Transformer.Type.FIELD)
public class CheckInOutFieldTransformer implements FieldTransformer {

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organisation id
     * @param fieldName The name of the field to use for the transformation
     * @param entity    The map of data to transform.
     * @return the transformed data
     */
    @Override
    public Object transform(final long orgId, final String fieldName, final Map<String, Object> entity) {
        final Optional<Object> optionalValue = Optional.ofNullable(entity.get(fieldName));

        return optionalValue.map(value -> Json.object((String) value).toMap()).orElseGet(() -> null);
    }
}