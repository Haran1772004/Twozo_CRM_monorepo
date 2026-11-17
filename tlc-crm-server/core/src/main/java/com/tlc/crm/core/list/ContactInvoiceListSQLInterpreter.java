package com.tlc.crm.core.list;

import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.i18n.I18nResolutionAdapter;

/**
 * <p>
 * Processes the entity list actions in the contact invoice list.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class ContactInvoiceListSQLInterpreter extends InvoiceListSQLInterpreter {

    public ContactInvoiceListSQLInterpreter(final Long userId, final Long orgId, final Provider provider,
                                            final JoinLoader joinLoader, final I18nResolutionAdapter i18NResolutionAdapter,
                                            final int maxFieldSelectionLimit, final int maxFilterCriteriaLimit) {
        super(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
    }

    @Override
    protected FieldConfig.FieldEnableType getFieldEnableType() {
        return FieldConfig.FieldEnableType.IS_ENABLED_FOR_CONTACT_INVOICE_LIST;
    }
}