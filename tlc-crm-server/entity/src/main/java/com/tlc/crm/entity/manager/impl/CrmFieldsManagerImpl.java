package com.tlc.crm.entity.manager.impl;

import com.tlc.crm.entity.models.internal.Field;
import com.tlc.sql.common.api.ds.OrgDataStore;

/**
 * <p>
 * Provides the {@link Field} related DB actions
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CrmFieldsManagerImpl extends AbstractCrmFieldsManager {

    private CrmFieldsManagerImpl() {
    }

    private static final class InstanceHolder {
        private static final CrmFieldsManagerImpl CRM_FIELDS_MANAGER = new CrmFieldsManagerImpl();
    }

    public static CrmFieldsManagerImpl getInstance() {
        return InstanceHolder.CRM_FIELDS_MANAGER;
    }

    /**
     * Provides the {@link OrgDataStore} for the given organisation ID.
     *
     * @param orgId Represents the organisation ID.
     * @return {@link OrgDataStore}
     */
    @Override
    protected OrgDataStore getOrgDataStore(final long orgId) {
        return orgDataStore(orgId);
    }
}
