package com.tlc.crm.security.internal.permission.model;

import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.SecureDefaultValueProvider;

/**
 * Provider for providing the current user's role ID
 *
 * @author Sathish
 * @version 1.0
 * @see SecureDefaultValueProvider
 */
public class CurrentUserRoleProvider implements SecureDefaultValueProvider<Long> {

    private static final String PROVIDER_NAME = "CURRENT_USER_ROLE_PROVIDER";

    private CurrentUserRoleProvider() {
    }

    private static final class Instance {
        private static final SecureDefaultValueProvider<Long> CURRENT_USER_PROVIDER = new CurrentUserRoleProvider();
    }

    /**
     * Gets the {@link CurrentUserRoleProvider} instance
     *
     * @return {@link CurrentUserRoleProvider}
     */
    public static SecureDefaultValueProvider<Long> getInstance() {
        return Instance.CURRENT_USER_PROVIDER;
    }

    /**
     * {@inheritDoc}
     *
     * @return the name of the value provider
     */
    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @return the user's role ID
     */
    @Override
    public Long get() {
        return LoggedInUserInfo.getUser().getRoleId();
    }
}