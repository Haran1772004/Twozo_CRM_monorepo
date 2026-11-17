package com.tlc.crm.core.features.quote.action.transformer;

import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;

import com.tlc.crm.core.features.quote.fields.QuotationFields;
import com.tlc.crm.core.features.quote.model.Quotation;
import com.tlc.crm.core.features.quote.util.QuotationURLGenerator;
import com.tlc.crm.security.iam.LoggedInUserInfo;

import java.util.Map;

/**
 * <p>
 * Manages quote action field related transformer actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Transformer(name = "quote_action", type = Transformer.Type.FIELD)
public class ActionFieldTransformer implements FieldTransformer {

    private final QuotationURLGenerator quotationUrlGenerator;

    public ActionFieldTransformer() {
        quotationUrlGenerator = QuotationURLGenerator.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organization id.
     * @param fieldName The name of the field to use for the transformation.
     * @param entity    The map of data to transform.
     * @return An Object representing the transformed.
     */
    @Override
    public Object transform(final long orgId, final String fieldName, final Map<String, Object> entity) {
        final Object quoteId = entity.get(fieldName);
        final Quotation.Status status = Quotation.Status.get((short) entity.get(QuotationFields.STATUS));

        return switch (status) {
            case PUBLISHED, VIEWED -> Map.of(
                    QuotationFields.LABEL, QuotationFields.VIEW_QUOTE,
                    QuotationFields.VALUE, quotationUrlGenerator.generatePreviewURL(orgId, LoggedInUserInfo.getUser().id(), (Long) quoteId)
            );
            default -> null;
        };
    }
}
