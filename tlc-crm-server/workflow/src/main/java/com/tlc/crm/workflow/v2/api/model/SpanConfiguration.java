package com.tlc.crm.workflow.v2.api.model;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

/**
 * Represents delay/wait span configuration for a workflow V2 node.
 * 
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class SpanConfiguration implements TlcModel {

    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.orgId.required")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.span.type.required")
    private SpanType type;

    private Integer amount;
    private PredefinedSpanUnit predefinedSpanUnit;
    private CustomSpanUnit customSpanUnit;
    private boolean skipWeekends;
    private String dueTime;
    private Long timestamp;

    public SpanType getType() {
        return type;
    }

    public void setType(final SpanType type) {
        this.type = type;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(final Integer amount) {
        this.amount = amount;
    }

    public CustomSpanUnit getCustomSpanUnit() {
        return customSpanUnit;
    }

    public void setCustomSpanUnit(final CustomSpanUnit customSpanUnit) {
        this.customSpanUnit = customSpanUnit;
    }

    public boolean getSkipWeekends() {
        return skipWeekends;
    }

    public void setSkipWeekends(final boolean skipWeekends) {
        this.skipWeekends = skipWeekends;
    }

    public String getDueTime() {
        return dueTime;
    }

    public void setDueTime(final String dueTime) {
        this.dueTime = dueTime;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final Long timestamp) {
        this.timestamp = timestamp;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public PredefinedSpanUnit getPredefinedSpanUnit() {
        return predefinedSpanUnit;
    }

    public void setPredefinedSpanUnit(final PredefinedSpanUnit predefinedSpanUnit) {
        this.predefinedSpanUnit = predefinedSpanUnit;
    }

    public short getUnit() {
        return switch (type) {
            case PREDEFINED -> predefinedSpanUnit.getId();
            case CUSTOM -> customSpanUnit.getId();
        };
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
        return null;
    }
}
