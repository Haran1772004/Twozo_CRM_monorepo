package com.tlc.crm.entity.action.transformer;

import com.tlc.crm.entity.fields.transformer.TransformerFields;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.formats.Formats;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.models.type.TimeRange;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.l10n.temporal.model.Temporal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages {@link TimeRange} related transformer actions.
 *
 * @author eswari
 * @version 1.0
 */
@Transformer(name = "timeRange", type = Transformer.Type.FIELD)
public class TimeRangeFieldTransformer implements FieldTransformer {

    private final FieldTypeService fieldTypeService;

    public TimeRangeFieldTransformer() {
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
            final Map<StoreSubType, FieldSubType> subTypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.TIME_RANGE);
            final String columnFormat = Formats.COLUMN_FORMAT.get();

            for (final Map.Entry<StoreSubType, FieldSubType> entry : subTypes.entrySet()) {
                final String name = entry.getKey().getAliasName();
                final Object value = entity.get(String.format(columnFormat, fieldName, name));

                if (value instanceof final Temporal temporal) {
                    transformedValue.put(name, temporal.getTimeStamp());
                }
            }
            transformedValue.put(TransformerFields.ID.getName(), fieldId);

            return transformedValue;
        }).orElseGet(() -> null);
    }
}