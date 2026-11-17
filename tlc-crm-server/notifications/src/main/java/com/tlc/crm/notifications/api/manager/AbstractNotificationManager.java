package com.tlc.crm.notifications.api.manager;

import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;

/**
 * <p>
 * Manages DB related actions for notification module.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public abstract class AbstractNotificationManager {

    protected AbstractNotificationManager() {
    }

    /**
     * <p>
     * Gets the {@link ReadableDataStore}
     * </p>
     *
     * @return {@link ReadableDataStore}
     */
    protected ReadableDataStore readableDataStore() {
        return DataStoreProvider.getReadableDataStore();
    }

    /**
     * <p>
     * Gets the org data store
     * </P>
     *
     * @param orgId Represents an organization id
     * @return The {@link OrgDataStore}
     */
    protected OrgDataStore orgDataStore(final long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }
}
