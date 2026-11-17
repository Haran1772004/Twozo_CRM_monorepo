package com.tlc.crm.common.service;

import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.crm.common.service.impl.UserPreferenceHolderServiceImpl;

/**
 * <p>
 * Offers the users to access their {@link UserPreferenceHolder}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface UserPreferenceHolderService {

    /**
     * <p>
     * Gets the {@link UserPreferenceHolderService} instance.
     * </p>
     *
     * @return {@link UserPreferenceHolderService}.
     */
    static UserPreferenceHolderService get() {
        return UserPreferenceHolderServiceImpl.getInstance();
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
}
