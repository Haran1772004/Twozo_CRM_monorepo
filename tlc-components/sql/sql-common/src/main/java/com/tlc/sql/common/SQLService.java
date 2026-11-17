package com.tlc.sql.common;

import com.tlc.commons.service.Service;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.api.ds.access.ReadAccessController;
import com.tlc.sql.common.api.ds.access.WriteAccessController;
import com.tlc.sql.common.api.meta.DatabaseType;

/**
 * Database-specific SQL service that extends {@link Service} with data store access.
 * Provides organization-scoped data stores with optional access control for secure operations.
 *
 * @author Abishek
 * @version 1.0
 */
public interface SQLService extends Service {

    /**
     * Returns an {@link OrgDataStore} for the specified organization.
     *
     * @param orgId the organization ID
     * @return the organization-scoped data store
     */
    OrgDataStore getOrgDataStore(Long orgId);

    /**
     * Gets the secure {@link OrgDataStore}, which authorizes tables and columns before processing queries
     *
     * @param orgId                 Represents the organization ID
     * @param writeAccessController The access controller responsible for validating read and write access to tables and columns
     * @return The secure {@link OrgDataStore}
     */
    OrgDataStore getSecureOrgDataStore(final long orgId, final WriteAccessController writeAccessController);

    /**
     * Gets the secure readable org data store, which authorizes tables and columns before processing queries
     *
     * @param orgId                Represents the organization ID
     * @param readAccessController The access controller responsible for validating read access to tables and columns
     * @return The secure org {@link ReadableDataStore}
     */
    ReadableDataStore getSecureReadableOrgDataStore(final long orgId, final ReadAccessController readAccessController);

    /**
     * Returns the administrative data store for schema-level and cross-org operations.
     *
     * @return the admin data store
     */
    AdminDataStore getAdminDataStore();

    /**
     * Returns the database type (OLTP or OLAP) that this service manages.
     *
     * @return the {@link DatabaseType}
     */
    DatabaseType getDatabaseType();
}