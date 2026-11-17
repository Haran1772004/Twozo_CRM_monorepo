package com.tlc.crm.entity.fieldaudit.models;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.validator.TlcModel;

import java.util.Objects;

/**
 * The model representing a single recorded field-level change.
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public final class FieldAuditEntry implements TlcModel {

    private Long id;
    private final Long orgId;
    private final short entitySourceId;
    private final Long entityId;
    private final Long fieldId;
    private final String previousValue;
    private final String newValue;
    private final Long updatedBy;
    private final long updatedAt;
    private final FieldAuditSource auditSource;
    private final Long auditSourceReferenceId;
    private final JsonObject metadata;

    private FieldAuditEntry(final Builder builder) {
        this.id = builder.id;
        this.orgId = builder.orgId;
        this.entitySourceId = builder.entitySourceId;
        this.entityId = builder.recordId;
        this.fieldId = builder.fieldId;
        this.previousValue = builder.previousValue;
        this.newValue = builder.newValue;
        this.updatedBy = builder.updatedBy;
        this.updatedAt = builder.updatedAt == 0L ? System.currentTimeMillis() : builder.updatedAt;
        this.auditSource = builder.auditSource;
        this.auditSourceReferenceId = builder.auditSourceReferenceId;
        this.metadata = builder.metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override public Object identity() {
        return null;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public short entitySourceId() {
        return entitySourceId;
    }

    public Long recordId() {
        return entityId;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public String previousValue() {
        return previousValue;
    }

    public String newValue() {
        return newValue;
    }

    public Long updatedBy() {
        return updatedBy;
    }

    public long updatedAt() {
        return updatedAt;
    }

    public FieldAuditSource auditSource() {
        return auditSource;
    }

    public Long auditSourceReferenceId() {
        return auditSourceReferenceId;
    }

    public JsonObject metadata() {
        return metadata;
    }

    public static final class Builder {
        private Long id;
        private Long orgId;
        private short entitySourceId;
        private Long recordId;
        private Long fieldId;
        private String previousValue;
        private String newValue;
        private Long updatedBy;
        private long updatedAt;
        private FieldAuditSource auditSource;
        private Long auditSourceReferenceId;
        private JsonObject metadata;

        public Builder id(final Long value) {
            this.id = value;
            return this;
        }

        public Builder orgId(final Long value) {
            this.orgId = value;
            return this;
        }

        public Builder entitySourceId(final short value) {
            this.entitySourceId = value;
            return this;
        }

        public Builder recordId(final Long value) {
            this.recordId = value;
            return this;
        }

        public Builder fieldId(final Long fieldId) {
            this.fieldId = fieldId;
            return this;
        }

        public Builder previousValue(final String value) {
            this.previousValue = value;
            return this;
        }

        public Builder newValue(final String value) {
            this.newValue = value;
            return this;
        }

        public Builder updatedBy(final Long value) {
            this.updatedBy = value;
            return this;
        }

        public Builder updatedAt(final long value) {
            this.updatedAt = value;
            return this;
        }

        public Builder auditSource(final FieldAuditSource auditSource) {
            this.auditSource = auditSource;
            return this;
        }

        public Builder auditSourceReferenceId(final Long auditSourceReferenceId) {
            this.auditSourceReferenceId = auditSourceReferenceId;
            return this;
        }

        public Builder metadata(final String value) {
            this.metadata = Objects.isNull(value) ? null : Json.object(value);
            return this;
        }

        public FieldAuditEntry build() {
            return new FieldAuditEntry(this);
        }
    }
}