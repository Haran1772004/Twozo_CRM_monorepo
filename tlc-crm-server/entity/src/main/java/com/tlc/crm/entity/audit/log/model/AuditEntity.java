package com.tlc.crm.entity.audit.log.model;

import java.util.Collection;

/**
 * <p>
 * A model that represents the audit entity details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public record AuditEntity(Long id, Long orgId, String name, AuditCategory auditCategory, Collection<AuditLog> auditLogs) {

    public static class Builder {

        private Long entityId;
        private Long orgId;
        private String entityName;
        private Collection<AuditLog> auditLogs;
        private AuditCategory auditCategory;

        public AuditEntity.Builder setEntityId(final Long entityId) {
            this.entityId = entityId;
            return this;
        }

        public AuditEntity.Builder setOrgId(final Long orgId) {
            this.orgId = orgId;
            return this;
        }

        public AuditEntity.Builder setEntityName(final String entityName) {
            this.entityName = entityName;
            return this;
        }

        public AuditEntity.Builder setAuditLogs(final Collection<AuditLog> auditLogs) {
            this.auditLogs = auditLogs;
            return this;
        }

        public AuditEntity.Builder setAuditCategory(final AuditCategory auditCategory) {
            this.auditCategory = auditCategory;
            return this;
        }

        public AuditEntity build() {
            return new AuditEntity(entityId, orgId, entityName, auditCategory, auditLogs);
        }
    }
}
