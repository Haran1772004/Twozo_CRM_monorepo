package com.tlc.crm.core.features.api.quota.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.features.validator.annotation.QuotaValidator;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.Valid;

import java.util.Objects;

/**
 * A holder class for managing quota instances
 *
 * @author kishore J
 * @version 1.0
 */
@QuotaValidator(groups = {Group.Create.class, Group.Update.class})
public class QuotaHolder implements TlcModel {

    private Long id;
    private Long orgId;

    @Valid
    private UserQuota userQuota;
    @Valid
    private TeamQuota teamQuota;

    public void setUserQuota(final UserQuota userQuota) {
        this.userQuota = userQuota;
    }

    public void setTeamQuota(final TeamQuota teamQuota) {
        this.teamQuota = teamQuota;
    }

    public TeamQuota getTeamQuota() {
        return  teamQuota;
    }

    public UserQuota getUserQuota() {
        return userQuota;
    }

    public boolean hasUserQuota() {
        return Objects.nonNull(userQuota);
    }

    public boolean hasTeamQuota() {
        return Objects.nonNull(teamQuota);
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
       throw ErrorCode.get(CoreErrorCodes.UNSUPPORTED_OPERATION);
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }
}