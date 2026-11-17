package com.tlc.crm.common.iam.api.manager.impl;

import com.tlc.crm.common.iam.api.manager.AbstractCrmUserManager;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.ds.OrgDataStore;

/**
 * <p>
 * A {@link SecureCrmUserManager} that provides the various methods for the {@link CrmUserManager} as a secured service.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see AbstractCrmUserManager
 */
public class SecureCrmUserManager extends AbstractCrmUserManager {

    private SecureCrmUserManager() {
    }

    private static class Instance {
        private static final SecureCrmUserManager INSTANCE = new SecureCrmUserManager();
    }

    /**
     * <p>
     * Gets the {@link SecureCrmUserManager} Instance
     * </p>
     *
     * @return The instance of {@link SecureCrmUserManager}
     */
    public static SecureCrmUserManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the org data store
     * </P>
     *
     * @param orgId Represents an organization id
     * @return The {@link OrgDataStore}
     */
    @Override
    protected OrgDataStore getDataStore(final long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }
}
