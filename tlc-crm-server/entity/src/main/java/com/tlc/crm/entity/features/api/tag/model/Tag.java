package com.tlc.crm.entity.features.api.tag.model;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.commons.color.Color;
import com.tlc.crm.entity.features.validator.annotation.TagValidator;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Model which represents the {@link Tag}
 *
 * @author EswariNivethaVU
 * @version 1.0
 */
@TagValidator(groups = {Group.Create.class, Group.Delete.class}, message = "i18n.tag.validation.failed")
public class Tag implements TlcModel, Serializable {
    private static final Color DEFAULT_COLOR = Color.SPRING_GREEN;

    @NotNull(groups = {Group.Update.class}, message = "i18n.tag.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @Size(max = 255, groups = {Group.Create.class, Group.Update.class}, message = "i18n.tag.name.max.size")
    private String name;

    private DateTime updatedTime;

    private DateTime createdTime;

    private DateTime lastUsed;

    private Set<Long> entityIds;

    @NotNull(groups = {Group.Update.class}, message = "i18n.tag.invalid.color")
    private Color color;

    private Short order;

    //TODO: Remove the transient once the spring migration is done
    private transient CrmUser createdBy;

    public CrmUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final CrmUser createdBy) {
        this.createdBy = createdBy;
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public Long orgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    @Override
    public Long id() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public DateTime getUpdatedTime() {
        return updatedTime;
    }

    public Set<Long> getEntityIds() {
        return entityIds;
    }

    public void addEntityId(final Long entityId) {
        this.entityIds = this.entityIds == null ? new HashSet<>() : this.entityIds;

        this.entityIds.add(entityId);
    }

    public void setUpdatedTime(final DateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Long getId() {
        return id;
    }

    public Long getOrgId() {
        return orgId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public DateTime getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(final DateTime lastUsed) {
        this.lastUsed = lastUsed;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(final Color color) {
        this.color = color;
    }

    public void setColor(final short colorId) {
        this.color = Color.get(colorId);
    }

    public void setColor(final String colorName) {
        this.color = Color.get(colorName);
    }

    public static Color getDefaultColor() {
        return DEFAULT_COLOR;
    }

    public Short getOrder() {
        return this.order;
    }

    public void setOrder(final Short order) {
        this.order = order;
    }

    @Override
    public Object identity() {
        return null;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final Tag tag = (Tag) object;
        return Objects.equals(id, tag.id) && Objects.equals(orgId, tag.orgId) && Objects.equals(name, tag.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orgId, name);
    }
}