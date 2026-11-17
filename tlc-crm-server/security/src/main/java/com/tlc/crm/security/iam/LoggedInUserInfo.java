package com.tlc.crm.security.iam;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.security.context.SecurityContext;
import com.tlc.crm.security.internal.context.SecurityContextHolder;
import com.tlc.crm.security.internal.status.SecurityErrorCodes;

import java.util.Objects;

/**
 * Utility to provide information about the user currently logged in
 *
 * @author sundar
 * @version 1.0
 */
public final class LoggedInUserInfo {

    private LoggedInUserInfo() {
    }

    /**
     * Gets the currently logged in {@link SecurityContext}
     *
     * @return the currently logged in {@link SecurityContext}
     */
    public static SecurityContext getSecurityContext() {

        if (!SecurityContext.exists()) {
            throw ErrorCode.get(SecurityErrorCodes.SECURITY_CONTEXT_NOT_FOUND);
        }
        return SecurityContextHolder.getInstance().getContext().orElseThrow(() -> ErrorCode.get(SecurityErrorCodes.SECURITY_CONTEXT_NOT_FOUND));
    }

    /**
     * Gets the currently logged in {@link BaseCrmUser}
     *
     * @return the currently logged in {@link BaseCrmUser}
     */
    public static <T extends BaseCrmUser> T getUser() {
        final BaseCrmUser user = getSecurityContext().getUser();

        if (Objects.isNull(user)) {
            throw ErrorCode.get(SecurityErrorCodes.USER_NOT_FOUND);
        }
        return (T) user;
    }

    /**
     * Checks whether the {@link SecurityContext} is existing or not.
     *
     * @return {@link SecurityContext} exists flag.
     */
    public static boolean hasSecurityContext() {
        return SecurityContext.exists();
    }
}
