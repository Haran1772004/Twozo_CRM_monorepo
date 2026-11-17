package com.tlc.crm.core.list;

import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.entity.list.EntitySQLListInterpreter;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.i18n.I18nResolutionAdapter;

/**
 * <p>
 * Provides the SQL list interpreter for segment contact list actions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class SegmentContactSQLListInterpreter extends EntitySQLListInterpreter {

    public SegmentContactSQLListInterpreter(final Long userId, final Long orgId, final Provider provider,
                                            final JoinLoader joinLoader, final I18nResolutionAdapter i18NResolutionAdapter,
                                            final int maxFieldSelectionLimit, final int maxFilterCriteriaLimit) {
        super(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
    }

    /**
     * <p>
     * Gets the field enablement type used for segment contact selected-column persistence.
     * </p>
     *
     * @return The {@link FieldConfig.FieldEnableType}
     */
    @Override
    protected FieldConfig.FieldEnableType getFieldEnableType() {
        return FieldConfig.FieldEnableType.IS_ENABLED_FOR_SEGMENT_CONTACT_LIST;
    }
}
