package com.tlc.crm.entity.action.transformer;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.fields.transformer.TransformerFields;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.formats.Formats;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Manages the territory related transformer actions.
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@Transformer(name = "territory", type = Transformer.Type.FIELD)
public class TerritoryFieldTransformer implements FieldTransformer {

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organisation id.
     * @param fieldName  The name of the field to use for the transformation
     * @param entity The map of data to transform
     * @return The {@link JsonObject} contains transformed value
     */
    @Override
    public Object transform(final long orgId, final String fieldName, final Map<String, Object> entity) {
        final Optional<Object> optValue = Optional.ofNullable(entity.get(fieldName));

        return optValue.map(fieldId -> {
            final Map<String, Object> transformedValue = new HashMap<>();
            final String nameKey = TransformerFields.NAME.getName();

            transformedValue.put(TransformerFields.ID.getName(), fieldId);
            transformedValue.put(nameKey, entity.get(String.format(Formats.COLUMN_FORMAT.get(), fieldName, nameKey)));

            return transformedValue;
        }).orElseGet(() -> null);
    }
}
