package com.tlc.crm.entity.features.api.activity.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.crm.icon.model.Icon;
import com.tlc.crm.common.action.search.annotation.SearchField;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Model which represents the ActivityType
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see TlcModel
 */
public class ActivityType implements TlcModel, Cloneable {

    @SearchField(name = "id", readMethodName = "id")
    @NotNull(groups = {Group.Delete.class}, message = "i18n.activity.activityType.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Delete.class}, message = "i18n.activity.activityType.invalid.orgId")
    private Long orgId;

    @SearchField(name = "name")
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.activityType.name.notNull")
    @Size(max = 50, groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.activityType.name.max.size")
    private String name;

    private Long ownerId;

    private boolean isActive;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.activityType.invalid.icon.id")
    private Icon activityIcon;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Icon getActivityIcon() {
        return activityIcon;
    }

    public void setActivityIcon(final Icon activityIcon) {
        this.activityIcon = activityIcon;
    }

    public boolean getActive() {
        return isActive;
    }

    public void setActive(final boolean active) {
        isActive = active;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(final Long ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public Long id() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActivityType clone() {
        try {
            return (ActivityType) super.clone();
        } catch (CloneNotSupportedException e) {
            throw ErrorCode.get(ErrorCodes.NOT_SUPPORTED, e);
        }
    }
}
