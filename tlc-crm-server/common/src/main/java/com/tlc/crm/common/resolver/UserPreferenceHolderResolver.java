package com.tlc.crm.common.resolver;

import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.crm.common.resolver.impl.UserPreferenceHolderResolverImpl;

/**
 * <p>
 * Resolves the preferences of the user.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface UserPreferenceHolderResolver {

    /**
     * <p>
     * Gets the {@link UserPreferenceHolderResolver} instance.
     * </p>
     *
     * @return {@link UserPreferenceHolderResolver}.
     */
    static UserPreferenceHolderResolver get() {
        return UserPreferenceHolderResolverImpl.getInstance();
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
