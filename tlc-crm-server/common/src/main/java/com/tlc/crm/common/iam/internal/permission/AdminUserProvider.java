package com.tlc.crm.common.iam.internal.permission;

import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.ScopeParticipant;
import com.tlc.crm.security.permission.model.SecureDefaultValueProvider;

/**
 * Value provider to provide the admin user ID
 *
 * @author sundar
 * @version 1.0
 */
public final class AdminUserProvider implements SecureDefaultValueProvider<Long> {

    private static final String PROVIDER_NAME = "ADMIN_USER_PROVIDER";

    private final UserService userService;

    private AdminUserProvider() {
        this.userService = UserService.getInstance();
    }

    private static final class Instance {
        private static final SecureDefaultValueProvider<Long> ADMIN_USER_PROVIDER = new AdminUserProvider();
    }

    /**
     * Gets the {@link AdminUserProvider} instance
     *
     * @return {@link AdminUserProvider}
     */
    public static SecureDefaultValueProvider<Long> getInstance() {
        return Instance.ADMIN_USER_PROVIDER;
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
     * @return The user ID associated with the {@link ScopeParticipant}
     */
    @Override
    public Long get() {
        return userService.getAdminUserId(LoggedInUserInfo.getUser().orgId());
    }
}