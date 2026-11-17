package com.tlc.crm.entity.fieldaudit.manager;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.entity.fieldaudit.manager.internal.FieldAuditManagerImpl;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditEntry;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditSource;
import com.tlc.crm.entity.fieldaudit.models.AuditFilter;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;

/**
 * Manager contract for {@link FieldAuditEntry}.
 *
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public interface FieldAuditManager extends ConfigManager<FieldAuditEntry> {

    /**
     * Returns the singleton {@link FieldAuditManager}.
     */
    static FieldAuditManager getInstance() {
        return FieldAuditManagerImpl.getInstance();
    }

    Table getTable();

    /**
     * Persist many history records in a single SQL transaction.
     *
     */
    void loadFieldAuditIntoContainer(Long orgId, Collection<FieldAuditEntry> events);

    /**
     * Page through history rows that match the supplied filter, ordered by
     * {@code UPDATED_AT DESC}.
     */
    Collection<FieldAuditEntry> fetchRecords(Long orgId, AuditFilter filter);

    /**
     * Total number of rows that match the supplied filter (used for pagination metadata).
     */
    long fetchCount(Long orgId, AuditFilter filter);

    /**
     * Marks the history records associated with the given source and reference IDs as inactive.
     * This is typically used when the source record is deleted, ensuring that the associated history records are not considered in active history queries.
     *
     * @param orgId         Represents the organization ID
     * @param source        Represents the source of the field changes.
     * @param referenceIds  Represents the collection of source reference IDs whose associated history records need to be deactivated.
     */
    void deactivateAuditSource(final Long orgId, final FieldAuditSource source, final Collection<Long> referenceIds);
}