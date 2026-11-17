package com.tlc.crm.entity.fieldaudit.manager.internal;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.entity.fieldaudit.cache.FieldTrackingConfigCache;
import com.tlc.crm.entity.fieldaudit.internal.status.FieldAuditErrorCodes;
import com.tlc.crm.entity.fieldaudit.keys.FieldAuditKeys;
import com.tlc.crm.entity.fieldaudit.manager.FieldTrackingConfigManager;
import com.tlc.crm.entity.fieldaudit.models.FieldTrackingConfig;
import com.tlc.crm.entity.fieldaudit.sql.resource.OMFATRACKINGCONFIG;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.validator.ProcessOutcome;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Default {@link FieldTrackingConfigManager} implementation.
 *
 * <p>Business logic only — cache operations are delegated to {@link FieldTrackingConfigCache}.</p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class FieldTrackingConfigManagerImpl extends AbstractConfigManager<FieldTrackingConfig> implements FieldTrackingConfigManager {

    private final FieldTrackingConfigCache trackingConfigCache;

    /**
     * Private constructor for singleton initialization.
     *
     * <p>Initializes the cache instance for managing field tracking configurations.</p>
     */
    private FieldTrackingConfigManagerImpl() {
        trackingConfigCache = FieldTrackingConfigCache.getInstance();
    }

    /**
     * Singleton instance holder using Bill Pugh pattern.
     */
    private static final class Instance {
        private static final FieldTrackingConfigManagerImpl FIELD_TRACKING_CONFIG_MANAGER = new FieldTrackingConfigManagerImpl();
    }

    /**
     * Retrieves the singleton instance of {@link FieldTrackingConfigManagerImpl}.
     *
     * @return the singleton instance
     */
    public static FieldTrackingConfigManagerImpl getInstance() {
        return Instance.FIELD_TRACKING_CONFIG_MANAGER;
    }

    /**
     * Retrieves the database table used for field tracking configurations.
     *
     * @return the {@link Table} instance representing OMFATrackingConfig
     */
    @Override
    public Table getTable() {
        return Table.get(OMFATRACKINGCONFIG.TABLE);
    }

    /**
     * Retrieves the field tracking configuration for the specified organization and source,
     * or returns a disabled default configuration if none exists.
     *
     * @param orgId    the organization ID
     * @param sourceId the entity source (e.g., CONTACT, COMPANY, DEAL)
     * @return the {@link FieldTrackingConfig} for the org/source pair, or a disabled default
     */
    @Override
    public FieldTrackingConfig getOrDefault(final Long orgId, final short sourceId) {
        final FieldTrackingConfig cached = trackingConfigCache.get(orgId, sourceId);

        if (Objects.nonNull(cached)) {
            return cached;
        }
        final FieldTrackingConfig fieldTrackingConfig = getFieldTrackingConfig(orgId, sourceId);
        final FieldTrackingConfig currentTrackingConfig = Objects.isNull(fieldTrackingConfig) ? FieldTrackingConfig.getDefaultConfig(orgId, sourceId) : fieldTrackingConfig;

        trackingConfigCache.put(orgId, sourceId, currentTrackingConfig);

        return currentTrackingConfig;
    }

    /**
     * Checks whether a specific field is tracked for the given organization and source.
     *
     * @param orgId     the organization ID
     * @param sourceId  the entity source (e.g., CONTACT, COMPANY, DEAL)
     * @param fieldName the field alias name to check (must not be null)
     * @return {@code true} if the field is tracked; {@code false} if null fieldName or not tracked
     */
    @Override
    public boolean isTracked(final Long orgId, final short sourceId, final String fieldName) {
        if (Objects.isNull(fieldName)) {
            return false;
        }
        return getOrDefault(orgId, sourceId).isFieldTracked(fieldName);
    }

    /**
     * Updates the set of tracked fields for the specified organization and source.
     *
     * @param orgId         the organization ID
     * @param userId        the ID of the user performing the update
     * @param sourceId      the entity source (e.g., CONTACT, COMPANY, DEAL)
     * @param trackedFields the set of field alias names to track (may be null or empty)
     */
    @Override
    public void update(final Long orgId, final Long userId, final short sourceId, final Set<String> trackedFields) {
        final Set<String> fields = Objects.isNull(trackedFields) ? Collections.emptySet() : new LinkedHashSet<>(trackedFields);

        if (fields.size() > FieldTrackingConfig.MAX_TRACKED_FIELDS_PER_MODULE) {
            throw ErrorCode.get(FieldAuditErrorCodes.TRACKED_FIELDS_LIMIT_EXCEEDED, "i18n.field.audit.tracked.fields.limit.exceeded");
        }

        try {
            final FieldTrackingConfig existingTrackingConfig = getFieldTrackingConfig(orgId, sourceId);
            final FieldTrackingConfig currentTrackingConfig = new FieldTrackingConfig(Objects.isNull(existingTrackingConfig) ? null : existingTrackingConfig.id(),
                    orgId, sourceId, fields, System.currentTimeMillis(), userId);

            persist(currentTrackingConfig);
            trackingConfigCache.put(orgId, sourceId, currentTrackingConfig);
        } catch (Throwable throwable) {
            LOGGER.error("Field tracking config upsert failed for org={} source={}", orgId, sourceId, throwable);
            throw ErrorCode.get(FieldAuditErrorCodes.CONFIG_UPDATE_FAILED);
        }
    }

    /**
     * Invalidates the cached tracking configuration for the specified organization and source.
     *
     * @param orgId    the organization ID
     * @param sourceId the entity source (e.g., CONTACT, COMPANY, DEAL)
     */
    @Override
    public void invalidate(final Long orgId, final short sourceId) {
        trackingConfigCache.invalidate(orgId, sourceId);
    }

    /**
     * Creates a new field tracking configuration in the database and caches it.
     *
     *
     * @param model the {@link FieldTrackingConfig} to create
     * @return the same model instance (for fluency)
     */
    @Override
    public FieldTrackingConfig create(final FieldTrackingConfig model) {
        persist(model);
        trackingConfigCache.put(model.orgId(), model.sourceId(), model);

        return model;
    }

    /**
     * Retrieves field tracking configurations by their primary keys.
     *
     * @param orgId the organization ID
     * @param ids the collection of primary key IDs to retrieve (may be null or empty)
     * @return a collection of {@link FieldTrackingConfig} objects; empty collection if
     *         ids is null, empty, or no matches found
     */
    @Override
    public Collection<FieldTrackingConfig> get(final Long orgId, final Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyList();
        }
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));
        return orgDataStore(orgId).get(selectQuery).getRows(table).map(this::rowToModel).toList();
    }

    /**
     * Retrieves partial field tracking configurations by their primary keys.
     *
     * @param orgId the organization ID
     * @param ids the collection of primary key IDs to retrieve
     * @return a collection of {@link FieldTrackingConfig} objects
     */
    @Override
    public Collection<FieldTrackingConfig> partialGet(final Long orgId, final Collection<Long> ids) {
        return get(orgId, ids);
    }

    /**
     * Updates multiple field tracking configurations in the database and caches them.
     *
     *
     * @param orgId the organization ID
     * @param models the collection of {@link FieldTrackingConfig} objects to update
     * @return a {@link ProcessOutcome} containing IDs of successfully processed records
     */
    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<FieldTrackingConfig> models) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        for (final FieldTrackingConfig model : models) {
            try {
                persist(model);
                trackingConfigCache.put(model.orgId(), model.sourceId(), model);
                processOutcome.addProcessedRecord(model.id());
            } catch (Throwable throwable) {
                LOGGER.error("Failed updating tracking config id={} org={}", model.id(), orgId, throwable);
                throw ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED, "i18n.common.update.failed");
            }
        }
        return processOutcome;
    }

    /**
     * Retrieves the field tracking configuration for the specified organization and source
     * from the database.
     *
     * @param orgId    the organization ID
     * @param sourceId the entity source (e.g., CONTACT, COMPANY, DEAL)
     * @return the {@link FieldTrackingConfig} for the org/source pair, or null if not found
     */
    private FieldTrackingConfig getFieldTrackingConfig(final Long orgId, final short sourceId) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMFATRACKINGCONFIG.SOURCE_ID), sourceId)));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(table);

        return Objects.isNull(row) ? null : rowToModel(row);
    }

    /**
     * Persists a field tracking configuration to the database.
     *
     *
     * @param config the {@link FieldTrackingConfig} to persist
     */
    private void persist(final FieldTrackingConfig config) {
        final Table table = getTable();
        final OrgDataStore dataStore = orgDataStore(config.orgId());
        final DataContainer container = DataContainer.create();
        final Row row;

        if (Objects.isNull(config.id())) {
            row = new Row(table);

            row.setOrgId(config.orgId());
            config.setId(row.getPKValue());
        } else {
            row = dataStore.get(table, List.of(config.id())).getRow(table, new WhereClause(Criteria.eq(table.getPKColumn(), config.id())));
        }
        row.set(OMFATRACKINGCONFIG.SOURCE_ID, config.sourceId());
        row.set(OMFATRACKINGCONFIG.TRACKED_FIELDS, encodeFields(config.trackedFields()));
        row.set(OMFATRACKINGCONFIG.UPDATED_AT, config.updatedAt());
        row.set(OMFATRACKINGCONFIG.UPDATED_BY, config.updatedBy());

        container.updateRow(row);
        dataStore.commitChanges(container);
    }

    /**
     * Converts a database row to a {@link FieldTrackingConfig} model object.
     *
     *
     * @param row the database row to convert
     * @return the corresponding {@link FieldTrackingConfig} model
     */
    private FieldTrackingConfig rowToModel(final Row row) {
        return new FieldTrackingConfig(row.getPKValue(), row.getOrgId(),
                ((Number) row.get(OMFATRACKINGCONFIG.SOURCE_ID)).shortValue(),
                decodeFields(row.get(OMFATRACKINGCONFIG.TRACKED_FIELDS)),
                ((Number) row.get(OMFATRACKINGCONFIG.UPDATED_AT)).longValue(),
                row.get(OMFATRACKINGCONFIG.UPDATED_BY));
    }

    /**
     * Encodes a set of field names into a comma-delimited string for database storage.
     *
     *
     * @param fields the set of field alias names to encode (may be null or empty)
     * @return a comma-delimited string, or null if fields is null or empty
     */
    private static String encodeFields(final Set<String> fields) {
        if (Objects.isNull(fields) || fields.isEmpty()) {
            return null;
        }
        return String.join(FieldAuditKeys.COMMA, fields);
    }

    /**
     * Decodes a comma-delimited string into a set of field names.
     *
     *
     * @param value the encoded field string (may be null)
     * @return a {@link LinkedHashSet} of field alias names; empty set if value is null or empty
     */
    private static Set<String> decodeFields(final Object value) {
        if (Objects.isNull(value)) {
            return Collections.emptySet();
        }
        final String string = value.toString().trim();

        if (string.isEmpty()) {
            return Collections.emptySet();
        }
        return Stream.of(string.split(FieldAuditKeys.COMMA))
                .map(String::trim)
                .filter(fields -> !fields.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}