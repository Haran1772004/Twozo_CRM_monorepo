package com.tlc.crm.messenger.internal.sync.manager;

import com.tlc.crm.messenger.sql.resource.OMOAFBREFRESHABLETOKEN;
import com.tlc.sql.common.api.dml.Table;

/**
 * Manages the Facebook sync related database actions
 *
 * @author Maharaja S
 * @version 1.0
 */
public class FBSyncManager extends AbstractSyncManager {

    private FBSyncManager() {
    }

    private static final class InstanceHolder {
        private static final FBSyncManager INSTANCE = new FBSyncManager();
    }

    /**
     * <p>
     * Gets the {@link FBSyncManager} instance
     * </p>
     *
     * @return {@link FBSyncManager} instance
     */
    public static FBSyncManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Gets the fb token table
     * </p>
     *
     * @return the fb token table
     */
    protected Table getTokenTable() {
        return Table.get(OMOAFBREFRESHABLETOKEN.TABLE);
    }
}
