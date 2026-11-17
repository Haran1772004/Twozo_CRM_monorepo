package com.tlc.crm.common.cache;

import com.tlc.crm.common.cache.internal.UserPreferenceHolderCacheImpl;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;

/**
 * <p>
 * Stores the preference of the user.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface UserPreferenceHolderCache extends RemoteCache {

    /**
     * <p>
     * Gets the {@link UserPreferenceHolderCache} instance.
     * </p>
     *
     * @return {@link UserPreferenceHolderCache}.
     */
    static UserPreferenceHolderCache get() {
        return UserPreferenceHolderCacheImpl.getInstance();
    }

    /**
     * <p>
     * Gets the {@link UserPreferenceHolder}.
     * </p>
     *
     * @param orgId  An organization's id.
     * @param userId A user's id.
     * @return {@link UserPreferenceHolder}.
     */
    UserPreferenceHolder get(final long orgId, final long userId);

    /**
     * Adds {@link UserPreferenceHolder}.
     *
     * @param userId               Represents a user's id.
     * @param userPreferenceHolder {@link UserPreferenceHolder}.
     */
    void add(final long userId, final UserPreferenceHolder userPreferenceHolder);

    /**
     * <p>
     * Removes the user's preference from cache.
     * </p>
     *
     * @param orgId  Represents an organization's id.
     * @param userId Represents a user's id.
     */
    void remove(final long orgId, final Long userId);
}
