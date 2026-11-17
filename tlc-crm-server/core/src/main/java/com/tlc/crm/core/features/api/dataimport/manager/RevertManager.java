package com.tlc.crm.core.features.api.dataimport.manager;

import com.tlc.crm.core.features.api.dataimport.manager.impl.RevertManagerImpl;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;

/**
 * Manages the data revert related DB actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface RevertManager {

    /**
     * Gets the instance of {@link RevertManager}.
     *
     * @return {@link RevertManager}.
     */
    static RevertManager getInstance() {
        return RevertManagerImpl.getInstance();
    }

    /**
     * Gets the total count of imported records from the history table.
     *
     * @param orgId    Refers the organization id.
     * @param importId Refers the import id.
     * @return The total count.
     */
    long getImportedRecordsCount(final long orgId, final long importId);

    /**
     * Reverts the imported data associated with the given import id.
     *
     * @param dataHolder Represents the {@link DataHolder}.
     * @param importId   Represents the data import id.
     */
    void revert(final DataHolder dataHolder, final long importId);
}
