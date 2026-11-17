package com.tlc.crm.common.iam.internal;

import com.tlc.crm.common.iam.UserDataTransferAction;

import java.util.Collection;

/**
 * <p>
 * Handles the user record migration process
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class UserRecordMigrationHandler {

    private final DataMigrationContainer dataMigrationContainer;

    private UserRecordMigrationHandler() {
        dataMigrationContainer = DataMigrationContainer.getInstance();
    }

    private static class Instance {
        private static final UserRecordMigrationHandler INSTANCE = new UserRecordMigrationHandler();
    }

    /**
     * <p>
     * Gets the {@link UserRecordMigrationHandler} Instance
     * </p>
     *
     * @return {@link UserRecordMigrationHandler}
     */
    public static UserRecordMigrationHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Transfer the user records from one user to another user
     * </p>
     *
     * @param orgId         Represents an org id
     * @param targetUserId  Represents the target user id
     * @param sourceUserIds Represents the source user ids
     */
    public void transferRecords(final Long orgId, final Long targetUserId, final Collection<Long> sourceUserIds) {

        for (final UserDataTransferAction userDataTransferAction : dataMigrationContainer.getRegisteredActions()) {
            userDataTransferAction.transferUserRecords(orgId, targetUserId, sourceUserIds);
        }
    }
}
