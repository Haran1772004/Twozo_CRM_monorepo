package com.tlc.crm.core.features.api.followers.user.manager;

import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.core.features.api.followers.user.manager.impl.UserFollowerManagerImpl;
import com.tlc.crm.core.features.api.followers.model.Follower;

import java.util.Collection;

/**
 * Manages UserFollower related DB actions
 *
 * @author Ryan
 * @version 1.0
 */
public interface UserFollowerManager {

    /**
     * Returns the singleton instance of {@link UserFollowerManager}.
     *
     * @return singleton {@link UserFollowerManager} instance
     */
    static UserFollowerManager getInstance() {
        return UserFollowerManagerImpl.getInstance();
    }

    /**
     * Update {@code OMUserFollowers} by adding and removing followers for a sales owner.
     *
     * @param orgId              organization id
     * @param salesOwnerId       sales owner user id
     * @param addedFollowerIds   follower ids to be added
     * @param removedFollowerIds follower ids to be removed
     */
    void update(final long orgId, final long salesOwnerId, final Collection<Long> addedFollowerIds,
                final Collection<Long> removedFollowerIds);

    /**
     * Fetches a page of followers configured for a sales owner.
     *
     * @param orgId        organization id
     * @param salesOwnerId sales owner user id
     * @param limitConfig  contains start and limit values for pagination
     * @return a Collection of {@link Follower} if present, otherwise empty
     */
    Collection<Follower> fetchFollowers(final long orgId, final long salesOwnerId, final LimitConfig limitConfig);

    /**
     * Returns the total number of followers configured for a sales owner.
     *
     * @param orgId        organization id
     * @param salesOwnerId sales owner user id
     * @return total follower count
     */
    long totalRecords(final long orgId, final long salesOwnerId);

    /**
     * Removes {@code OMUserFollowers} rows for users belonging to the given role
     *
     * @param orgId  Represents the organization id
     * @param roleId Represents the role id
     */
    void removeUserFollowersByRole(final long orgId, final long roleId);
}
