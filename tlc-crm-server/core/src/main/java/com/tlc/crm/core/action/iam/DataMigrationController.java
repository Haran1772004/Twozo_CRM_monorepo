package com.tlc.crm.core.action.iam;

import com.tlc.crm.common.iam.UserDataMigrator;
import com.tlc.crm.common.iam.UserDataTransferAction;
import com.tlc.crm.core.api.iam.manager.UserRecordMigrator;

import java.util.Collection;

/**
 * <p>
 * Transfers the given the {@link Collection} of user records to another user
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
@UserDataMigrator(name = "transferUserData")
public class DataMigrationController implements UserDataTransferAction {

    private final UserRecordMigrator userRecordMigrator;

    public DataMigrationController() {
        userRecordMigrator = UserRecordMigrator.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Represents an organization id
     * @param targetUserId  Represent the target user id to update
     * @param sourceUserIds Represents the {@link Collection} of source user ids to be updated
     */
    @Override
    public void transferUserRecords(final Long orgId, final Long targetUserId, final Collection<Long> sourceUserIds) {
        userRecordMigrator.transferUserRecords(orgId, targetUserId, sourceUserIds);
    }
}
