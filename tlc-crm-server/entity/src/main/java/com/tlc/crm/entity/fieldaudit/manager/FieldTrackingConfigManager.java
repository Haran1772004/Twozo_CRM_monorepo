package com.tlc.crm.entity.fieldaudit.manager;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.entity.fieldaudit.manager.internal.FieldTrackingConfigManagerImpl;
import com.tlc.crm.entity.fieldaudit.models.FieldTrackingConfig;

import com.tlc.sql.common.api.dml.Table;

import java.util.Set;

/**
 * Manager contract for the per (org, source) {@link FieldTrackingConfig}.
 *
 * <p>Validation rules:</p>
 * <ul>
 *   <li>At most {@value FieldTrackingConfig#MAX_TRACKED_FIELDS_PER_MODULE} fields per source.</li>
 *   <li>If {@code isTrackingEnabled} is {@code false}, no history rows are created.</li>
 *   <li>Configuration is org-scoped; a default ({@code disabled}) configuration is
 *       returned when no row exists for a given (org, source) pair.</li>
 * </ul>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public interface FieldTrackingConfigManager extends ConfigManager<FieldTrackingConfig> {

    /**
     * Returns the singleton {@link FieldTrackingConfigManager}.
     */
    static FieldTrackingConfigManager getInstance() {
        return FieldTrackingConfigManagerImpl.getInstance();
    }

    /**
     * The table for {@link FieldTrackingConfig} entries.
     */
    Table getTable();

    /**
     * Retrieve the current configuration; returns
     * {@link FieldTrackingConfig#getDefaultConfig(Long, short)} when none exists.
     */
    FieldTrackingConfig getOrDefault(Long orgId, short sourceId);

    /**
     * Upsert configuration for the given (org, source) pair.
     */
    void update(Long orgId, Long userId, short sourceId, Set<String> trackedFields);

    /**
     * Convenience: is the supplied field currently tracked for the (org, source) pair?
     */
    boolean isTracked(Long orgId, short sourceId, String fieldName);

    /**
     * Invalidate any cached entry (used after external/admin changes).
     */
    void invalidate(Long orgId, short sourceId);
}