package com.tlc.crm.security.sql;

import com.tlc.commons.util.Env;

import com.tlc.crm.security.internal.sql.ReadAccessControllerImpl;
import com.tlc.crm.security.internal.sql.WriteAccessControllerImpl;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.api.ds.SecureOrgDataStore;
import com.tlc.sql.common.api.ds.SecureReadableOrgDataStore;
import com.tlc.sql.common.api.ds.WritableDataStore;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.provider.SqlServiceProvider;

/**
 * An interface responsible for providing the required data store
 *
 * @author sundar
 * @version 1.0
 */
public interface DataStoreProvider {

    /**
     * Gets the {@link AdminDataStore}
     *
     * @return the {@link AdminDataStore}
     */
    static AdminDataStore getAdminDataStore() {
        return SqlServiceProvider.get().getAdminDataStore();
    }

    /**
     * Gets the {@link ReadableDataStore}
     *
     * @return {@link ReadableDataStore}
     */
    static ReadableDataStore getReadableDataStore() {
        return getAdminDataStore();
    }

    /**
     * Gets the {@link WritableDataStore}
     *
     * @return {@link WritableDataStore}
     */
    static WritableDataStore getWritableDataStore() {
        return getAdminDataStore();
    }

    /**
     * Gets the {@link OrgDataStore}
     *
     * @param orgId Represents the organization ID
     * @return {@link OrgDataStore}
     */
    static OrgDataStore getOrgDataStore(final long orgId) {
        return SqlServiceProvider.get().getOrgDataStore(orgId);
    }

    /**
     * Gets the {@link SecureReadableOrgDataStore} for the specified organization
     *
     * @param orgId Represents the organization ID
     * @return {@link SecureReadableOrgDataStore}
     */
    static ReadableDataStore getSecureReadableOrgDataStore(final long orgId) {
        return Env.isSecurityEnabled() ? SqlServiceProvider.get().getSecureReadableOrgDataStore(orgId, ReadAccessControllerImpl.getInstance())
                : getOrgDataStore(orgId);
    }

    /**
     * Gets the {@link SecureOrgDataStore} for the specified organization
     *
     * @param orgId Represents the organization ID
     * @return {@link SecureOrgDataStore}
     */
    static OrgDataStore getSecureOrgDataStore(final long orgId) {
        return Env.isSecurityEnabled() ? SqlServiceProvider.get().getSecureOrgDataStore(orgId, WriteAccessControllerImpl.getInstance())
                : getOrgDataStore(orgId);
    }


    /**
     * Gets the {@link AdminDataStore}
     *
     * @return the {@link AdminDataStore}
     */
    static AdminDataStore getAdminDataStore(final DatabaseType databaseType) {
        return SqlServiceProvider.getService(databaseType).getAdminDataStore();
    }

    /**
     * Gets the {@link ReadableDataStore}
     *
     * @return {@link ReadableDataStore}final DatabaseType databaseType
     */
    static ReadableDataStore getReadableDataStore(final DatabaseType databaseType) {
        return getAdminDataStore(databaseType);
    }

    /**
     * Gets the {@link WritableDataStore}
     *
     * @return {@link WritableDataStore}
     */
    static WritableDataStore getWritableDataStore(final DatabaseType databaseType) {
        return getAdminDataStore(databaseType);
    }

    /**
     * Gets the {@link OrgDataStore}
     *
     * @param orgId Represents the organization ID
     * @return {@link OrgDataStore}
     */
    static OrgDataStore getOrgDataStore(final long orgId, final DatabaseType databaseType) {
        return SqlServiceProvider.getService(databaseType).getOrgDataStore(orgId);
    }

    /**
     * Gets the {@link SecureReadableOrgDataStore} for the specified organization
     *
     * @param orgId Represents the organization ID
     * @return {@link SecureReadableOrgDataStore}
     */
    static ReadableDataStore getSecureReadableOrgDataStore(final long orgId, final DatabaseType databaseType) {
        return Env.isSecurityEnabled() ? SqlServiceProvider.getService(databaseType).getSecureReadableOrgDataStore(orgId, ReadAccessControllerImpl.getInstance())
                : getOrgDataStore(orgId, databaseType);
    }

    /**
     * Gets the {@link SecureOrgDataStore} for the specified organization
     *
     * @param orgId Represents the organization ID
     * @return {@link SecureOrgDataStore}
     */
    static OrgDataStore getSecureOrgDataStore(final long orgId, final DatabaseType databaseType) {
        return Env.isSecurityEnabled() ? SqlServiceProvider.getService(databaseType).getSecureOrgDataStore(orgId, WriteAccessControllerImpl.getInstance())
                : getOrgDataStore(orgId, databaseType);
    }
}