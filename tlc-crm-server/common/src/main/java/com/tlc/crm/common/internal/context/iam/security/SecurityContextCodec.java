package com.tlc.crm.common.internal.context.iam.security;

import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.security.context.AbstractSecurityContextCodec;
import com.tlc.crm.security.iam.BaseCrmUser;

/**
 * Codec to encode and decode the security context with user details
 *
 * @author sundar
 * @version 1.0
 * @see AbstractSecurityContextCodec
 */
public class SecurityContextCodec extends AbstractSecurityContextCodec {

    private final UserService userService;

    public SecurityContextCodec() {
        this.userService = UserService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization ID
     * @param userId Represents the user ID
     * @return the {@link BaseCrmUser}
     */
    @Override
    protected BaseCrmUser getUser(final long orgId, final long userId) {
        return userService.getUser(orgId, userId);
    }
}
