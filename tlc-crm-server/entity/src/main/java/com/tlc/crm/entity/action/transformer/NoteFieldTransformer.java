package com.tlc.crm.entity.action.transformer;

import com.tlc.crm.entity.fields.transformer.TransformerFields;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.formats.Formats;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Manages note related transformer actions.
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
@Transformer(name = "note", type = Transformer.Type.FIELD)
public class NoteFieldTransformer implements FieldTransformer {

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organisation id.
     * @param fieldName  The name of the field to use for the transformation
     * @param entity The map of data to transform
     * @return The transformed value
     */
    @Override
    public Object transform(final long orgId, final String fieldName, final Map<String, Object> entity) {
        final Optional<Object> optValue = Optional.ofNullable(entity.get(fieldName));

        return optValue.map(fieldId -> {
            final Map<String, Object> transformedValue = new HashMap<>();
            transformedValue.put(TransformerFields.ID.getName(), fieldId);
            transformedValue.put(TransformerFields.NAME.getName(),
                    entity.get(String.format(Formats.COLUMN_FORMAT.get(), fieldName, TransformerFields.NOTE.getName())));

            return transformedValue;
        }).orElseGet(() -> null);
    }
}
