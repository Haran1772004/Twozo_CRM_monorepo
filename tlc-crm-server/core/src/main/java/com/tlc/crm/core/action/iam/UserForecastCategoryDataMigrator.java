package com.tlc.crm.core.action.iam;

import com.tlc.crm.common.iam.UserDataMigrator;
import com.tlc.crm.common.iam.UserDataTransferAction;
import com.tlc.crm.entity.forecastcategory.manager.ForecastCategoryManager;
import com.tlc.crm.security.iam.LoggedInUserInfo;

import java.util.Collection;

/**
 * Handles migration of forecast category–related user data during user transfer or deletion.
 *
 * @author kishore J
 * @version 1.0
 */
@UserDataMigrator(name = "transferUserForecastCategoryData")
public class UserForecastCategoryDataMigrator implements UserDataTransferAction {

    private final ForecastCategoryManager forecastCategoryManager;

    public UserForecastCategoryDataMigrator() {
        forecastCategoryManager = ForecastCategoryManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Represents an org id
     * @param targetUserId  Represents the crm user id to update
     * @param sourceUserIds Represents the {@link Collection} of source user ids to be updated
     */
    @Override
    public void transferUserRecords(final Long orgId, final Long targetUserId, final Collection<Long> sourceUserIds) {
        forecastCategoryManager.transferUserRecords(orgId, LoggedInUserInfo.getUser().id(), sourceUserIds);
    }
}
