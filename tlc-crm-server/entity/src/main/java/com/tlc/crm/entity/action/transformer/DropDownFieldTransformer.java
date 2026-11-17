package com.tlc.crm.entity.action.transformer;

import com.tlc.crm.entity.fields.transformer.TransformerFields;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.formats.Formats;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages dropdown related transformer actions.
 *
 * @author eswari
 * @version 1.0
 */
@Transformer(name = "dropdown", type = Transformer.Type.FIELD)
public class DropDownFieldTransformer implements FieldTransformer {

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organisation id.
     * @param fieldName  the name of the field to use for the transformation
     * @param entity the map of data to transform
     * @return the {@link Map} contains transformed value
     */
    @Override
    public Map<String, Object> transform(final long orgId, final String fieldName, final Map<String, Object> entity) {
        final Optional<Object> optValue = Optional.ofNullable(entity.get(fieldName));

        return optValue.map(fieldId -> {
            final Map<String, Object> transformedValue = new HashMap<>();
            final String option = TransformerFields.OPTION.getName();

            transformedValue.put(TransformerFields.ID.getName(), fieldId);
            transformedValue.put(option, entity.get(String.format(Formats.COLUMN_FORMAT.get(), fieldName, option)));

            return transformedValue;
        }).orElseGet(() -> null);
    }
}