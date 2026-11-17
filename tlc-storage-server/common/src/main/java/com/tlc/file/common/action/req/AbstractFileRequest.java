package com.tlc.file.common.action.req;

import com.tlc.commons.code.ErrorCode;
import com.tlc.file.common.status.FileErrorCodes;
import com.tlc.iam.common.models.User;

import com.tlc.web.WebExchange;
import com.tlc.web.action.req.AbstractHttpRequest;

import java.util.Objects;

/**
 * Provides abstract services to manage the request.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 * @see FileRequest
 */
public abstract class AbstractFileRequest extends AbstractHttpRequest implements FileRequest {

    private User user;

    protected AbstractFileRequest(final WebExchange exchange) {
        super(exchange);
    }

    /**
     * {@inheritDoc}
     *
     * @param user Represents the {@link User}
     */
    @Override
    public void setUser(final User user) {
        this.user = user;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link User}
     */
    @Override
    public User getUser() {
        if (Objects.nonNull(user)) {
            return user;
        }
        throw ErrorCode.get(FileErrorCodes.USER_NOT_FOUND);
    }

    /**
     * {@inheritDoc}
     *
     * @return The organization id
     */
    @Override
    public Long orgId() {
        return getUser().orgId();
    }

    /**
     * {@inheritDoc}
     *
     * @return the user ID
     */
    @Override
    public Long userId() {
        return getUser().id();
    }


}
