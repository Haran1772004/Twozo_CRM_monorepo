package com.tlc.crm.common.service.impl;

import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.crm.common.resolver.UserPreferenceHolderResolver;
import com.tlc.crm.common.service.UserPreferenceHolderService;

/**
 * <p>
 * Provides the service to retrieve {@link UserPreferenceHolder}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class UserPreferenceHolderServiceImpl implements UserPreferenceHolderService {

    private final UserPreferenceHolderResolver userPreferenceHolderResolver;

    private UserPreferenceHolderServiceImpl() {
        userPreferenceHolderResolver = UserPreferenceHolderResolver.get();
    }

    private static class Instance {
        private static final UserPreferenceHolderServiceImpl INSTANCE = new UserPreferenceHolderServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link UserPreferenceHolderServiceImpl} instance.
     * </p>
     *
     * @return {@link UserPreferenceHolderServiceImpl}.
     */
    public static UserPreferenceHolderServiceImpl getInstance() {
        return UserPreferenceHolderServiceImpl.Instance.INSTANCE;
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
    @Override
    public UserPreferenceHolder get(final long orgId, final long userId) {
        return userPreferenceHolderResolver.get(orgId, userId);
    }
}
