package com.tlc.file.common.action.req;

import com.tlc.iam.common.models.User;
import com.tlc.web.action.req.HttpRequest;

/**
 * Holds HTTP request's information.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface FileRequest extends HttpRequest {

    /**
     * Gets the orgId.
     *
     * @return The organisation id.
     */
    Long orgId();

    /**
     * Gets the userId.
     *
     * @return The user id.
     */
    Long userId();

    /**
     * <p>
     * Sets the crm user
     * </p>
     *
     * @param user Represents the {@link User}
     */
    void setUser(final User user);

    /**
     * <p>
     * Gets the {@link User}
     * </p>
     *
     * @return The {@link User}
     */
    User getUser();
}
