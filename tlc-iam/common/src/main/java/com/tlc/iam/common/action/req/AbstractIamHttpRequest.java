package com.tlc.iam.common.action.req;

import com.tlc.iam.common.models.User;
import com.tlc.web.WebExchange;
import com.tlc.web.action.req.AbstractHttpRequest;

import java.util.Objects;

/**
 * Provides abstract services to manage the request
 *
 * @author Ashok Selvakumar
 * @version 1.0
 * @see IamHttpRequest
 */
public abstract class AbstractIamHttpRequest extends AbstractHttpRequest implements IamHttpRequest {

    private User user;

    protected AbstractIamHttpRequest(final WebExchange exchange) {
        super(exchange);
    }

    /**
     * Returns org id of an user
     *
     * @return Org Id
     */
    @Override
    public Long orgId() {
        return Objects.isNull(user) ? null : user.orgId();
    }

    /**
     * Returns user id
     *
     * @return User id
     */
    @Override
    public Long userId() {
        return Objects.isNull(user) ? null : user.id();
    }

    /**
     * Sets the requested user's details to the HTTP request
     *
     * @param user Represents the {@link User}
     */
    @Override
    public void setUser(final User user) {
        this.user = user;
    }

    /**
     * Requested user
     *
     * @return {@link User}
     */
    @Override
    public User getUser() {
        return this.user;
    }
}