package com.tlc.crm.entity.action.transformer;

import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.formats.Formats;
import com.tlc.crm.entity.fields.transformer.TransformerFields;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages {@link Email} related transformer actions.
 *
 * @author eswari
 * @version 1.0
 */
@Transformer(name = "email", type = Transformer.Type.FIELD)
public class EmailFieldTransformer implements FieldTransformer {

    private final FieldTypeService fieldTypeService;

    public EmailFieldTransformer() {
        fieldTypeService = FieldTypeService.getInstance();
    }

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
            final Collection<FieldSubType> fieldSubTypes = fieldTypeService.getSubTypesByStoreType(StoreType.EMAIL);
            final String columnFormat = Formats.COLUMN_FORMAT.get();

            for (final FieldSubType fieldSubType : fieldSubTypes) {
                final String name = fieldSubType.storeSubType().getAliasName();
                final Object value = entity.get(String.format(columnFormat, fieldName, name));
                final StoreSubType storeSubType = fieldSubType.storeSubType();

                transformedValue.put(name, StoreSubType.EMAIL_TYPE == storeSubType
                        ? (value instanceof Integer id ? Email.Type.valueOf(id).getDisplayName() : value) : value);
            }
            transformedValue.put(TransformerFields.ID.getName(), fieldId);

            return transformedValue;
        }).orElseGet(() -> null);
    }
}