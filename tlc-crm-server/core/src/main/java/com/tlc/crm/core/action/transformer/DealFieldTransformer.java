package com.tlc.crm.core.action.transformer;

import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.formats.Formats;
import com.tlc.crm.entity.fields.transformer.TransformerFields;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Manages deal related transformer actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Transformer(name = "deal", type = Transformer.Type.FIELD)
public class DealFieldTransformer implements FieldTransformer {

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organisation id.
     * @param fieldName  The name of the field to use for the transformation
     * @param entity The map of data to transform
     * @return The {@link Map} contains transformed value
     */
    @Override
    public Map<String, Object> transform(final long orgId, final String fieldName, final Map<String, Object> entity) {
        final Optional<Object> optValue = Optional.ofNullable(entity.get(fieldName));

        return optValue.map(fieldId -> {
            final Map<String, Object> transformedValue = new HashMap<>();
            final String titleKey = TransformerFields.TITLE.getName();
            final String dealValueKey = TransformerFields.VALUE.getName();
            final String pipelineStageKey = TransformerFields.PIPELINE_STAGE.getName();
            final String expectedCloseDateKey = TransformerFields.EXPECTED_CLOSE_DATE.getName();
            final String columnFormat = Formats.COLUMN_FORMAT.get();

            transformedValue.put(TransformerFields.ID.getName(), fieldId);
            transformedValue.put(titleKey, entity.get(String.format(columnFormat, fieldName, titleKey)));
            transformedValue.put(dealValueKey, entity.get(String.format(columnFormat, fieldName, dealValueKey)));
            transformedValue.put(pipelineStageKey, entity.get(String.format(columnFormat, fieldName, pipelineStageKey)));
            transformedValue.put(expectedCloseDateKey, entity.get(String.format(columnFormat, fieldName, expectedCloseDateKey)));

            return transformedValue;
        }).orElseGet(() -> null);
    }
}