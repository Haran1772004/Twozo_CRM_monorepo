package com.tlc.crm.entity.action.transformer;

import com.tlc.crm.entity.fields.transformer.TransformerFields;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.formats.Formats;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * It transforms the location field entry into required forms.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Transformer(name = "location", type = Transformer.Type.FIELD)
public class LocationFieldTransformer implements FieldTransformer {

    private final FieldTypeService fieldTypeService;

    public LocationFieldTransformer() {
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
            final Map<StoreSubType, FieldSubType> subTypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.ADDRESS);
            final StringBuilder value = new StringBuilder();
            final String columnFormat = Formats.COLUMN_FORMAT.get();

            for (final Iterator<Map.Entry<StoreSubType, FieldSubType>> iterator = subTypes.entrySet().iterator(); iterator.hasNext(); ) {
                final Map.Entry<StoreSubType, FieldSubType> entry = iterator.next();

                value.append(entity.get(String.format(columnFormat, fieldName, entry.getKey().getAliasName())));

                if (iterator.hasNext()) {
                    value.append(", ");
                }
            }
            transformedValue.put(TransformerFields.ID.getName(), fieldId);
            transformedValue.put(fieldName, value.toString());

            return transformedValue;
        }).orElseGet(() -> null);
    }
}
