package com.tlc.crm.entity.fieldaudit.models;


import com.tlc.sql.common.api.dml.OrderByClause;

import java.util.Collections;
import java.util.Set;

/**
 * Filter criteria used by
 * {@code FieldAuditManager#findByFilter(Long, AuditFilter)}.
 *
 * <p>Supported filters: field names, user ids, change-source set, time range and
 * pagination. Default ordering is {@code UPDATED_AT DESC}.</p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class AuditFilter {

    public static final int DEFAULT_START = 1;
    public static final int DEFAULT_SIZE = 25;
    public static final int MAX_SIZE = 200;

    private final short sourceId;
    private final Long entityId;
    private final Set<Long> fieldIds;
    private final Set<Long> userIds;
    private final Set<FieldAuditSource> fieldAuditSources;
    private final Long fromTimestamp;
    private final Long toTimestamp;
    private final int start;
    private final int size;
    private final String sortField;
    private final OrderByClause.OrderType orderType;

    private AuditFilter(final Builder builder) {
        this.sourceId = builder.sourceId;
        this.entityId = builder.entityId;
        this.fieldIds = nullSafe(builder.fieldIds);
        this.userIds = nullSafe(builder.userIds);
        this.fieldAuditSources = nullSafe(builder.fieldAuditSources);
        this.fromTimestamp = builder.fromTimestamp;
        this.toTimestamp = builder.toTimestamp;
        this.start = builder.start <= 0 ? DEFAULT_START : builder.start;
        this.size = builder.size <= 0 ? DEFAULT_SIZE : Math.min(builder.size, MAX_SIZE);
        this.sortField = builder.sortField;
        this.orderType = builder.orderType;
    }

    private static <T> Set<T> nullSafe(final Set<T> in) {
        return in == null ? Collections.emptySet() : Collections.unmodifiableSet(in);
    }

    public short sourceId() {
        return sourceId;
    }

    public Long entityId() {
        return entityId;
    }

    public Set<Long> fieldIds() {
        return fieldIds;
    }

    public Set<Long> userIds() {
        return userIds;
    }

    public Set<FieldAuditSource> changeSources() {
        return fieldAuditSources;
    }

    public Long fromTimestamp() {
        return fromTimestamp;
    }

    public Long toTimestamp() {
        return toTimestamp;
    }

    public int start() {
        return start;
    }

    public int size() {
        return size;
    }

    public String sortField() {
        return sortField;
    }

    public OrderByClause.OrderType orderType() {
        return orderType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private short sourceId;
        private Long entityId;
        private Set<Long> fieldIds;
        private Set<Long> userIds;
        private Set<FieldAuditSource> fieldAuditSources;
        private Long fromTimestamp;
        private Long toTimestamp;
        private int start;
        private int size;
        private String sortField;
        private OrderByClause.OrderType orderType;

        public Builder sourceId(final short sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public Builder entityId(final Long entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder fieldIds(final Set<Long> fieldIds) {
            this.fieldIds = fieldIds;
            return this;
        }

        public Builder userIds(final Set<Long> userIds) {
            this.userIds = userIds;
            return this;
        }

        public Builder changeSources(final Set<FieldAuditSource> changeSources) {
            this.fieldAuditSources = changeSources;
            return this;
        }

        public Builder fromTimeStamp(final Long fromTimestamp) {
            this.fromTimestamp = fromTimestamp;
            return this;
        }

        public Builder toTimestamp(final Long toTimestamp) {
            this.toTimestamp = toTimestamp;
            return this;
        }

        public Builder start(final int start) {
            this.start = start;
            return this;
        }

        public Builder size(final int size) {
            this.size = size;
            return this;
        }

        public Builder sortField(final String sortField) {
            this.sortField = sortField;
            return this;
        }

        public Builder sortType(final OrderByClause.OrderType orderType) {
            this.orderType = orderType;
            return this;
        }

        public AuditFilter build() {
            return new AuditFilter(this);
        }
    }
}