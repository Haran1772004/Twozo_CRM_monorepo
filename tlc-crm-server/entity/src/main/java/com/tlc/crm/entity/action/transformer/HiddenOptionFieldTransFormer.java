package com.tlc.crm.entity.action.transformer;

import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.formats.Formats;
import com.tlc.crm.entity.fields.transformer.TransformerFields;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Manages hidden option related transformer actions
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@Transformer(name = "hiddenOption", type = Transformer.Type.FIELD)
public class HiddenOptionFieldTransFormer implements FieldTransformer {

    /**
     * Transforms the data in the given map based on the specified field.
     *
     * @param orgId     Represents the organisation id.
     * @param fieldName The name of the field to use for the transformation.
     * @param entity    The map of data to transform.
     * @return An Object representing the transformed.
     */
    @Override
    public Object transform(final long orgId, final String fieldName, final Map<String, Object> entity) {
        final Optional<Object> value = Optional.ofNullable(entity.get(fieldName));

        return value.map(fieldId -> {
            final Map<String, Object> transformedValue = new HashMap<>();
            final String option = TransformerFields.OPTION.getName();

            transformedValue.put(TransformerFields.ID.getName(), fieldId);
            transformedValue.put(option, entity.get(String.format(Formats.COLUMN_FORMAT.get(), fieldName, option)));

            return transformedValue;
        }).orElseGet(() -> null);
    }
}
