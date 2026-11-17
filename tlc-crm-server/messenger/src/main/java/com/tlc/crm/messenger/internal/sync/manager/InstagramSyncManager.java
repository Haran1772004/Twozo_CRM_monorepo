package com.tlc.crm.messenger.internal.sync.manager;

import com.tlc.crm.messenger.sql.resource.OMOAINSTAGRAMREFRESHABLETOKEN;
import com.tlc.sql.common.api.dml.Table;

/**
 * <p>
 * Pertains to Instagram sync related database actions.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class InstagramSyncManager extends AbstractSyncManager {

    private InstagramSyncManager() {
    }

    private static final class InstanceHolder {
        private static final InstagramSyncManager INSTANCE = new InstagramSyncManager();
    }

    /**
     * <p>
     * Gets {@link InstagramSyncManager}.
     * </p>
     *
     * @return {@link InstagramSyncManager}.
     */
    public static InstagramSyncManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Gets the instagram token table.
     * </p>
     *
     * @return the instagram token table.
     */
    @Override
    protected Table getTokenTable() {
        return Table.get(OMOAINSTAGRAMREFRESHABLETOKEN.TABLE);
    }
}