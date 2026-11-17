package com.tlc.iam.server.internal.models;

import com.tlc.iam.common.models.User;
import com.tlc.iam.server.internal.api.manager.UserManager;

import java.io.Serializable;
import java.util.Objects;

/**
 * Holds user data
 *
 * @author Selvakumar
 * @version 1.0
 */
public class UserHolder implements Serializable {

    private final long userId, orgId;
    private User user;

    public UserHolder(long userId, long orgId) {
        this.userId = userId;
        this.orgId = orgId;
    }

    /**
     * A resolver that resolves the user data
     *
     * @return
     */
    public User resolve() {
        if (Objects.isNull(user)) {
            user = UserManager.getInstance().getUser(userId, orgId);
        }

        return user;
    }

    public long getUserId() {
        return userId;
    }

    public long getOrgId() {
        return orgId;
    }
}
