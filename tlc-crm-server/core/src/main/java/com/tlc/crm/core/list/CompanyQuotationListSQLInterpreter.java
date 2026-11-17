package com.tlc.crm.core.list;

import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.entity.list.EntitySQLListInterpreter;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.i18n.I18nResolutionAdapter;

/**
 * <p>
 * Process the entity list actions in the company quotation list and provides the implementation for the {@link EntitySQLListInterpreter}.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public final class CompanyQuotationListSQLInterpreter extends EntitySQLListInterpreter {

    public CompanyQuotationListSQLInterpreter(final Long userId, final Long orgId, final Provider provider,
                                              final JoinLoader joinLoader, final I18nResolutionAdapter i18NResolutionAdapter, final int maxFieldSelectionLimit, final int maxFilterCriteriaLimit) {
        super(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
    }

    /**
     * <p>
     * Gets the {@link FieldConfig.FieldEnableType}
     * </p>
     *
     * @return The {@link FieldConfig.FieldEnableType}
     */
    @Override
    protected FieldConfig.FieldEnableType getFieldEnableType() {
        return FieldConfig.FieldEnableType.IS_ENABLED_FOR_COMPANY_QUOTATION_LIST;
    }
}