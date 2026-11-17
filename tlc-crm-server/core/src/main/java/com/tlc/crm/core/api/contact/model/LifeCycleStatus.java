package com.tlc.crm.core.api.contact.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditEntityName;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditField;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Update;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <p>
 * A model that represents the life cycle status.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class LifeCycleStatus implements TlcModel {

    @NotNull(groups = {Delete.class}, message = "i18n.lifecycle.status.invalid.id")
    private Long id;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @AuditEntityName
    @AuditField(aliasName = "name", displayName = "i18n.contact.fields.status")
    @Size(min = 3, max = 100, groups = {Create.class, Update.class}, message = "i18n.contact.status.invalid.name")
    private String name;

    private Label label;

    private Long stageId;

    private Short order;

    private boolean isSystemStatus;

    private boolean isDefault;

    @Override
    public Long id() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(final Label label) {
        this.label = label;
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(final Long stageId) {
        this.stageId = stageId;
    }

    public Short getOrder() {
        return order;
    }

    public void setOrder(final Short order) {
        this.order = order;
    }

    public boolean isSystemStatus() {
        return isSystemStatus;
    }

    public void setSystemStatus(final boolean systemStatus) {
        isSystemStatus = systemStatus;
    }

    public void setDefault(final boolean isDefault) {
        this.isDefault = isDefault;
    }

    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public Object identity() {
        return null;
    }

    public enum Label {

        CLOSED_LOST((short) 1),
        CLOSED_WON((short) 2),
        ;

        private final short id;

        Label(final short id) {
            this.id = id;
        }

        public static Label get(final short id) {
            return switch (id) {
                case 1 -> CLOSED_LOST;
                case 2 -> CLOSED_WON;
                default -> throw ErrorCode.get(ContactErrorCodes.INVALID_LABEL, String.format("Invalid input ID: %d", id));
            };
        }

        public short getId() {
            return id;
        }
    }
}
