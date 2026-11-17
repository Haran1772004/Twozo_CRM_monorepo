package com.tlc.crm.core.features.invoice.action.transformer;

import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.core.features.invoice.fields.InvoiceFields;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * <p>
 * Transforms the invoice due date and issued date into the number of days overdue.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@Transformer(name = "invoice_days_overdue", type = Transformer.Type.FIELD)
public class DaysOverdueFieldTransformer implements FieldTransformer {

    /**
     * <p>
     * Calculates the number of days between the invoice issued date and due date.
     * </p>
     *
     * @param orgId     Represents the organization ID.
     * @param fieldName Represents the name of the field being transformed.
     * @param entity    Represents the invoice field values.
     * @return The number of days overdue, or {@code 0} when the required values are unavailable or invalid.
     */
    @Override
    public Object transform(final long orgId, final String fieldName, final Map<String, Object> entity) {
        final Object dueBy = entity.get(InvoiceFields.DUE_DATE);
        final Object issuedAt = entity.get(InvoiceFields.INVOICE_DATE);

        if (dueBy == null || issuedAt == null) {
            return 0;
        }

        try {
            final Instant dueByInstant = Instant.parse(dueBy.toString());
            final Instant issueInstant = Instant.parse(issuedAt.toString());

            return Math.max(0, ChronoUnit.DAYS.between(issueInstant, dueByInstant));
        } catch (final Exception exception) {
            return 0;
        }
    }
}