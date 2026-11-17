package com.tlc.crm.core.features.invoice.action.transformer;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.core.features.invoice.fields.InvoiceFields;
import com.tlc.crm.core.features.invoice.model.InvoiceType;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import java.util.Locale;
import java.util.Map;

/**
 * <p>
 * Transforms the invoice billing frequency value into a structured representation
 * containing the billing frequency identifier, name, and display name.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@Transformer(name = "invoice_billing_frequency", type = Transformer.Type.FIELD)
public class BillingFrequencyFieldTransformer implements FieldTransformer {

    private final UserService crmUserService;
    private final I18nService i18nService;

    public BillingFrequencyFieldTransformer() {
        crmUserService = UserService.getInstance();
        i18nService = I18nServiceProvider.get();
    }

    /**
     * <p>
     * Transforms the invoice billing frequency value from the entity into a structured
     * representation containing its identifier, name, and display name.
     * </p>
     *
     * @param orgId     Represents the organization ID.
     * @param fieldName Represents the field name containing the billing frequency value.
     * @param entity    Represents the invoice entity data.
     * @return A map containing the billing frequency ID, name, and display name,
     * or {@code null} if the billing frequency cannot be resolved.
     */
    @Override
    public Object transform(final long orgId, final String fieldName, final Map<String, Object> entity) {
        final Object invoiceType = entity.get(fieldName);

        if (invoiceType == null) {
            return null;
        }
        final InvoiceType resolvedType = InvoiceType.get(((Number) invoiceType).shortValue());

        if (resolvedType == null) {
            return null;
        }

        return Map.of(
                InvoiceFields.ID, resolvedType.getValue(),
                InvoiceFields.DISPLAY_NAME, resolveI18nValue(resolvedType.getI18nKey())
        );
    }

    /**
     * <p>
     * Resolves the localized i18n value for the given key.
     * </p>
     *
     * @param key The i18n key to resolve
     * @return The localized value corresponding to the given key
     */
    protected String resolveI18nValue(final String key) {
        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final UserRegionalPreference userRegionalPreference = crmUserService.getUser(crmUser.orgId(), crmUser.id())
                .getPreferenceHolder().get(UserRegionalPreference.TYPE);

        return i18nService.getResolver(Locale.forLanguageTag(userRegionalPreference.getLanguage().getName())).get(key);
    }
}