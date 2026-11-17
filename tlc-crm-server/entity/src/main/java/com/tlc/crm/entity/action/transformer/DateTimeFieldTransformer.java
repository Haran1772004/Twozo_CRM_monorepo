package com.tlc.crm.entity.action.transformer;

import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.l10n.temporal.model.DateTime;

import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Transforms date time fields according to user's preference time.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@Transformer(name = "dateTime", type = Transformer.Type.FIELD)
public class DateTimeFieldTransformer implements FieldTransformer {

    public DateTimeFieldTransformer() {
    }

    /**
     * {@inheritDoc}.
     *
     * @param orgId     Represents the organisation id.
     * @param fieldName The name of the field to use for the transformation.
     * @param entity    The map of data to transform.
     * @return
     */
    @Override
    public Object transform(final long orgId, final String fieldName, final Map<String, Object> entity) {
        final Optional<Object> optValue = Optional.ofNullable(entity.get(fieldName));

        return optValue.map(fieldValue -> ((DateTime) fieldValue).getTimeStamp()).orElseGet(() -> null);
    }
}
