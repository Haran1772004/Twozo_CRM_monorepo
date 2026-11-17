package com.tlc.crm.auto.assignment.api.model;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

/**
 * A model that represents the {@link AssignmentRuleUser} details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AssignmentRuleUser {

    private Long id;
    private Territory territory;
    private CrmUser crmUser;
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.auto.assignment.rule.user.required")
    private Long userId;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Territory getTerritory() {
        return territory;
    }

    public void setTerritory(final Territory territory) {
        this.territory = territory;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public CrmUser getCrmUser() {
        return crmUser;
    }

    public void setCrmUser(final CrmUser crmUser) {
        this.crmUser = crmUser;
    }
}
