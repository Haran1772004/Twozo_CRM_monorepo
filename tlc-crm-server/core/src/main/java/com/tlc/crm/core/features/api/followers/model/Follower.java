package com.tlc.crm.core.features.api.followers.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.features.internal.followers.status.SourceFollowersErrorCode;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.validator.TlcModel;

import java.util.Collection;

/**
 * <p>
 * A model that represents the {@link Follower} details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class Follower implements TlcModel {

    private final Long userId;
    private long addedAt;
    private Long orgId;
    private CrmUser crmUser;
    private Collection<RolePermission> permissions;

    public Follower(final Long id) {
        this.userId = id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(final Long addedAt) {
        this.addedAt = addedAt;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    @Override
    public Long id() {
        throw ErrorCode.get(SourceFollowersErrorCode.UNSUPPORTED_OPERATION);
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    public CrmUser getCrmUser() {
        return crmUser;
    }

    public void setCrmUser(final CrmUser crmUser) {
        this.crmUser = crmUser;
    }

    public Collection<RolePermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(final Collection<RolePermission> permissions) {
        this.permissions = permissions;
    }
}
