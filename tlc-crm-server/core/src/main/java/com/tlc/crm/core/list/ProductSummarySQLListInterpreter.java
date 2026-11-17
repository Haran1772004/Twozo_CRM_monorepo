package com.tlc.crm.core.list;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.list.EntitySQLListInterpreter;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.i18n.I18nResolutionAdapter;

/**
 * <p>
 * Process the entity list actions in the product summary and Provides the implementation for the {@link EntitySQLListInterpreter}.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class ProductSummarySQLListInterpreter extends EntitySQLListInterpreter {

    public ProductSummarySQLListInterpreter(final Long userId, final Long orgId, final Provider provider, final JoinLoader joinLoader,
                                            final I18nResolutionAdapter i18NResolutionAdapter, final int maxFieldSelectionLimit, final int maxFilterCriteriaLimit) {
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
        return switch (provider) {
            case DEAL_LIST -> FieldConfig.FieldEnableType.IS_ENABLED_FOR_PRODUCT_SUMMARY_DEAL_LIST;
            default -> throw ErrorCode.get(EntityErrorCodes.UNSUPPORTED_PROVIDER, "i18n.entity.list.provider.not.found");
        };
    }
}