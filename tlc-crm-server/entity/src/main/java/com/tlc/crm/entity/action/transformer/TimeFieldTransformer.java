package com.tlc.crm.entity.action.transformer;

import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.l10n.temporal.model.Time;


import java.util.Map;

/**
 * <p>
 * Transforms Time field by retrieving it's time stamp.
 * </p>
 *
 * @author Muthu kumar V.
 * @version 1.0.
 */
@Transformer(name = "time", type = Transformer.Type.FIELD)
public class TimeFieldTransformer implements FieldTransformer {

    /**
     * {@inheritDoc}.
     *
     * @param orgId     Represents the organisation id.
     * @param fieldName The name of the field to use for the transformation.
     * @param entity    The map of data to transform.
     * @return The transformed instance of time
     */
    @Override
    public Object transform(final long orgId, final String fieldName, final Map<String, Object> entity) {
        final Object fieldValue = entity.get(fieldName);

        if (fieldValue instanceof final Time time) {
            return time.getTimeStamp();
        }
        return null;
    }
}