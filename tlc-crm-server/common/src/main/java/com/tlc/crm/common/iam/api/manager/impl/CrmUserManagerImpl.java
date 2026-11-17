package com.tlc.crm.common.iam.api.manager.impl;

import com.tlc.crm.common.iam.api.manager.AbstractCrmUserManager;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.ds.OrgDataStore;

/**
 * <p>
 * A {@link CrmUserManagerImpl} that provides the various methods for the {@link CrmUserManager}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 * @see AbstractCrmUserManager
 */
public class CrmUserManagerImpl extends AbstractCrmUserManager {

    private CrmUserManagerImpl() {
    }

    private static class Instance {
        private static final CrmUserManagerImpl INSTANCE = new CrmUserManagerImpl();
    }

    /**
     * <p>
     * Gets the {@link CrmUserManagerImpl} Instance
     * </p>
     *
     * @return The instance of {@link CrmUserManagerImpl}
     */
    public static CrmUserManagerImpl getInstance() {
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
        return DataStoreProvider.getOrgDataStore(orgId);
    }
}
