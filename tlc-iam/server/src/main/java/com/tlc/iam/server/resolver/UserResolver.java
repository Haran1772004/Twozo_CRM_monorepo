package com.tlc.iam.server.resolver;

import com.tlc.iam.common.models.User;
import com.tlc.iam.server.resolver.internal.UserResolverImpl;

/**
 * A resolver that resolves the {@link User} related details
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface UserResolver {

    /**
     * <p>
     * Gets the instance of {@link  UserResolverImpl}
     * </p>
     *
     * @return the instance of {@link UserResolverImpl}
     */
    static UserResolverImpl getInstance() {
        return UserResolverImpl.getInstance();
    }

    /**
     * <p>
     * Gets the user by the specified organization id and user id
     * </p>
     *
     * @param iamOrgId  Represents the iam organization id
     * @param iamUserId Represents the iam user id
     * @return the {@link User}
     */
    User get(final Long iamOrgId, final Long iamUserId);

    /**
     * <p>
     * Adds the user to the cache
     * </p>
     *
     * @param user Represents the {@link User}
     */
    void add(final User user);

    /**
     * <p>
     * Removes the user from the cache
     * </p>
     *
     * @param iamUserId Represents the iam user id
     */
    void remove(final Long iamUserId);
}