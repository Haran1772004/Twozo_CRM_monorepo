package com.tlc.crm.security.internal.permission.model;

import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.SecureDefaultValueProvider;

/**
 * Provider for providing the current user ID
 *
 * @author sundar
 * @version 1.0
 * @see SecureDefaultValueProvider
 */
public class CurrentUserProvider implements SecureDefaultValueProvider<Long> {

    private static final String PROVIDER_NAME = "CURRENT_USER_PROVIDER";

    private CurrentUserProvider() {
    }

    private static final class Instance {
        private static final SecureDefaultValueProvider<Long> CURRENT_USER_PROVIDER = new CurrentUserProvider();
    }

    /**
     * Gets the {@link CurrentUserProvider} instance
     *
     * @return {@link CurrentUserProvider}
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
     * @return the user ID
     */
    @Override
    public Long get() {
        return LoggedInUserInfo.getUser().id();
    }
}