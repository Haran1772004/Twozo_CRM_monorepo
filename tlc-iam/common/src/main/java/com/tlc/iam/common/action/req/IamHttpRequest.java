package com.tlc.iam.common.action.req;

import com.tlc.iam.common.models.User;

import com.tlc.web.action.req.HttpRequest;

/**
 * Holds HTTP request's information
 *
 * @author Selvakumar G
 * @version 1.0
 */
public interface IamHttpRequest extends HttpRequest {

    /**
     * Returns org id of a user
     *
     * @return Org Id
     */
    Long orgId();

    /**
     * Returns user id
     *
     * @return User id
     */
    Long userId();

    /**
     * Sets the requested user's details to the HTTP request
     *
     * @param user Represents the {@link User}
     */
    void setUser(final User user);

    /**
     * Requested user
     *
     * @return {@link User}
     */
    User getUser();
}