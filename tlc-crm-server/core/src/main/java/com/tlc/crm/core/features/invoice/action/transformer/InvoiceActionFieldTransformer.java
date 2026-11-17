package com.tlc.crm.core.features.invoice.action.transformer;

import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.core.features.invoice.service.InvoiceURLGenerator;
import com.tlc.crm.core.features.invoice.fields.InvoiceFields;
import com.tlc.crm.security.iam.LoggedInUserInfo;

import java.util.Map;

/**
 * <p>
 * Transforms the invoice action field into a view invoice action containing the invoice preview URL.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@Transformer(name = "invoice_action", type = Transformer.Type.FIELD)
public class InvoiceActionFieldTransformer implements FieldTransformer {

    private final InvoiceURLGenerator invoiceURLGenerator;

    /**
     * <p>
     * Initializes the invoice action field transformer with the invoice URL generator.
     * </p>
     */
    public InvoiceActionFieldTransformer() {
        this.invoiceURLGenerator = InvoiceURLGenerator.getInstance();
    }

    /**
     * <p>
     * Transforms the invoice ID into an action containing the invoice preview URL.
     * </p>
     *
     * @param orgId     Represents the organization ID.
     * @param fieldName Represents the name of the field being transformed.
     * @param entity    Represents the invoice field values.
     * @return The invoice view action containing the preview URL, or {@code null} if the invoice ID is unavailable.
     */
    @Override
    public Object transform(final long orgId, final String fieldName, final Map<String, Object> entity) {
        final Object invoiceId = entity.get(fieldName);

        if (invoiceId == null) {
            return null;
        }

        return Map.of(
                InvoiceFields.LABEL, InvoiceFields.VIEW_INVOICE,
                InvoiceFields.VALUE, invoiceURLGenerator.generatePreviewURL(
                        orgId, LoggedInUserInfo.getUser().id(), ((Number) invoiceId).longValue())
        );
    }
}