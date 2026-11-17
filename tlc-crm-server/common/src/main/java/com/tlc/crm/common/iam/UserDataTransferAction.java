package com.tlc.crm.common.iam;

import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;

import java.util.Collection;

/**
 * <p>
 * An action which used to transfers the records of {@link Collection} of crm user ids
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public interface UserDataTransferAction {

    /**
     * <p>
     * Transfers the records of source user ids to target user id for the given organization
     * </p>
     *
     * @param orgId         Represents a {@link CrmOrg} id
     * @param targetUserId  Represents the {@link CrmUser} id to update
     * @param sourceUserIds Represents the {@link Collection} of source user ids to be updated
     */
    void transferUserRecords(final Long orgId, final Long targetUserId, final Collection<Long> sourceUserIds);
}
