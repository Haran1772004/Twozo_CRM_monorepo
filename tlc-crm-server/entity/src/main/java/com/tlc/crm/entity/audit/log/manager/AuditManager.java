package com.tlc.crm.entity.audit.log.manager;

import com.tlc.crm.entity.audit.log.manager.impl.AuditManagerImpl;
import com.tlc.crm.entity.audit.log.model.AuditEntity;
import com.tlc.crm.entity.audit.log.model.AuditLog;
import com.tlc.crm.entity.audit.log.model.AuditField;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Manages the audit log related DB/ES actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface AuditManager {

    static AuditManager getInstance() {
        return AuditManagerImpl.getInstance();
    }

    /**
     * <p>
     * Stores the audit log into the ES storage.
     * </p>
     *
     * @param orgId         Represents the organisation id.
     * @param auditEntities Holds the details of the audit entity.
     */
    void auditEntry(final Long orgId, final Collection<AuditEntity> auditEntities);

    /**
     * <p>
     * Fetches all the {@link AuditField} are available in the DB.
     * </p>
     *
     * @return Map of audit category and their fields
     */
    Map<String, Collection<AuditField>> getAllFields();

    /**
     * <p>
     * Fetches the count of the total records.
     * </p>
     *
     * @param orgId  Represents the org id.
     * @param userId Represents the user id.
     * @return Count of the total records.
     */
    long getTotalLogsCount(final Long orgId, final Long userId);

    /**
     * <p>
     * Fetches all the {@link AuditLog} for the given user
     * </p>
     *
     * @param orgId  Represents the org id
     * @param userId Represents the user id
     * @param start  Represents the start value
     * @param limit  Represents the limit value
     * @return The user's audit logs
     */
    Collection<AuditEntity> getUserAudits(final Long orgId, final Long userId, final int start, final int limit);
}
