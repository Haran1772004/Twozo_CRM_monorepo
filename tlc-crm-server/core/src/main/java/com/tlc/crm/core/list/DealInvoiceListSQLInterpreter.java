package com.tlc.crm.core.list;

import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.i18n.I18nResolutionAdapter;

/**
 * <p>
 * Processes the entity list actions in the deal invoice list.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class DealInvoiceListSQLInterpreter extends InvoiceListSQLInterpreter {

    /**
     * <p>
     * Initializes the deal invoice list SQL interpreter with the required list configuration.
     * </p>
     *
     * @param userId                  Represents the user ID.
     * @param orgId                   Represents the organization ID.
     * @param provider                Represents the entity list provider.
     * @param joinLoader              Represents the join loader.
     * @param i18NResolutionAdapter   Represents the i18n resolution adapter.
     * @param maxFieldSelectionLimit  Represents the maximum field selection limit.
     * @param maxFilterCriteriaLimit  Represents the maximum filter criteria limit.
     */
    public DealInvoiceListSQLInterpreter(final Long userId, final Long orgId, final Provider provider,
                                         final JoinLoader joinLoader, final I18nResolutionAdapter i18NResolutionAdapter,
                                         final int maxFieldSelectionLimit, final int maxFilterCriteriaLimit) {
        super(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
    }

    /**
     * <p>
     * Returns the field enable type applicable to the deal invoice list.
     * </p>
     *
     * @return The field enable type for the deal invoice list.
     */
    @Override
    protected FieldConfig.FieldEnableType getFieldEnableType() {
        return FieldConfig.FieldEnableType.IS_ENABLED_FOR_DEAL_INVOICE_LIST;
    }
}