package com.tlc.iam.server.service.internal;

import com.tlc.iam.common.models.User;
import com.tlc.iam.server.resolver.UserResolver;
import com.tlc.iam.server.service.IamUserService;

/**
 * Provides the implementation for {@link IamUserService}
 *
 * @author Maharaja S
 * @version 1.0
 */
public class IamUserServiceImpl implements IamUserService {

    private final UserResolver userResolver;

    private IamUserServiceImpl() {
        userResolver = UserResolver.getInstance();
    }

    private static final class Instance {
        private static final IamUserServiceImpl USER_SERVICE = new IamUserServiceImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link  IamUserServiceImpl}
     * </p>
     *
     * @return the instance of {@link IamUserServiceImpl}
     */
    public static IamUserServiceImpl getInstance() {
        return Instance.USER_SERVICE;
    }

    /**
     * {@inheritDoc}
     *
     * @param iamOrgId  Represents the iam organization id
     * @param iamUserId Represents the iam user id
     * @return the {@link User}
     */
    @Override
    public User get(final Long iamOrgId, final Long iamUserId) {
        return userResolver.get(iamOrgId, iamUserId);
    }
}
