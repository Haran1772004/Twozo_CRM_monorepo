package com.tlc.iam.server.service;

import com.tlc.iam.server.service.internal.IamUserServiceImpl;
import com.tlc.iam.common.models.User;

/**
 * Provides the {@link User} related services
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface IamUserService {

    /**
     * <p>
     * Gets the instance of the {@link IamUserServiceImpl}
     * </p>
     *
     * @return The {@link IamUserServiceImpl} instance
     */
    static IamUserService getInstance() {
        return IamUserServiceImpl.getInstance();
    }

    /**
     * <p>
     * Gets the {@link User} by the provided iam organization id and iam user id
     * </p>
     *
     * @param iamOrgId  Represents the iam organization id
     * @param iamUserId Represents the iam user id
     * @return the {@link User}
     */
    User get(final Long iamOrgId, final Long iamUserId);
}
