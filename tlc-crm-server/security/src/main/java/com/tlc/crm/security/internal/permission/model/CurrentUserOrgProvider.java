package com.tlc.crm.security.internal.permission.model;

import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.SecureDefaultValueProvider;

/**
 * Provider for providing the current user's organization ID.
 *
 * @author Sathish
 * @version 1.0
 * @see SecureDefaultValueProvider
 */
public class CurrentUserOrgProvider implements SecureDefaultValueProvider<Long> {

    private static final String PROVIDER_NAME = "CURRENT_USER_ORG_PROVIDER";

    private CurrentUserOrgProvider() {
    }

    private static final class Instance {
        private static final SecureDefaultValueProvider<Long> CURRENT_USER_PROVIDER = new CurrentUserOrgProvider();
    }

    /**
     * Gets the {@link CurrentUserOrgProvider} instance
     *
     * @return {@link CurrentUserOrgProvider}
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
     * @return the user's org ID
     */
    @Override
    public Long get() {
        return LoggedInUserInfo.getUser().orgId();
    }
}