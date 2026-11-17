package com.tlc.crm.entity.service.cleanup;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Service responsible for cleaning up orphan references in Custom Wide Tables
 * when referenced entities or options are deleted.
 * </p>
 * <p>
 * This service executes direct SQL UPDATE statements to clear orphan references,
 * avoiding the overhead of loading records into memory.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class OrphanReferenceCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrphanReferenceCleanupService.class);

    /**
     * Custom Wide Table names for each primary source
     */
    private static final Map<Source, String> SOURCE_TO_CUSTOM_TABLE = Map.of(
            Source.CONTACT, "MCOContactCustomFieldValues",
            Source.COMPANY, "MCOCompanyCustomFieldValues",
            Source.DEAL, "MDODealCustomFieldValues",
            Source.PRODUCT, "MPOProductCustomFieldValues"
    );

    private final FieldService fieldService;

    private OrphanReferenceCleanupService() {
        this.fieldService = FieldService.getInstance();
    }

    private static class Instance {
        private static final OrphanReferenceCleanupService INSTANCE = new OrphanReferenceCleanupService();
    }

    /**
     * Gets the singleton instance of the cleanup service.
     *
     * @return the singleton instance
     */
    public static OrphanReferenceCleanupService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Cleans up option references by nullifying or removing the deleted option ID
     * from the specific field column.
     */
    public void cleanupOptionReferences(final long orgId, final long optionId, final long fieldId) {
        final FieldInfo fieldInfo = fieldService.getField(orgId, fieldId);

        if (Objects.isNull(fieldInfo) || !fieldInfo.isPersistent()) {
            return;
        }
        final StoreType storeType = fieldInfo.storeType();
        final Column column = fieldInfo.column();
        final Table table = fieldInfo.table();

        if (Objects.equals(StoreType.MULTI_SELECT, storeType)) {
            cleanupMultiValueColumn(orgId, optionId, table, column);
        } else {
            cleanupSingleValueColumn(orgId, optionId, table, column);
        }
    }

    /**
     * Cleans up references for a given store type across all custom wide tables.
     */
    public void cleanupEntityReferences(final long orgId, final long deletedId, final StoreType storeType, final Source source) {
       try {
           final Collection<FieldInfo> affectedFields = getAffectedFields(orgId, source, storeType);

           if (affectedFields.isEmpty()) {
               return;
           }

           for (final FieldInfo fieldInfo : affectedFields) {
               cleanupSingleValueColumn(orgId, deletedId, fieldInfo.table(), fieldInfo.column());
           }
       } catch (final Exception exception) {
           LOGGER.error("Error cleaning up references for source {} and store type {}: {}", source, storeType, exception.getMessage(), exception);
       }
    }

    /**
     * Gets fields that can contain references to the given store type.
     */
    private Collection<FieldInfo> getAffectedFields(final long orgId, final Source source, final StoreType targetStoreType) {
        final Collection<FieldInfo> persistentFields = fieldService.getPersistentFieldsBySource(orgId, source.getName());

        return persistentFields.stream().filter(field -> Objects.equals(field.storeType(), targetStoreType))
                .collect(Collectors.toList());
    }

    /**
     * Cleans up a single-value column by setting it to NULL where it matches the deleted ID.
     */
    private void cleanupSingleValueColumn(final long orgId, final long deletedId, final Table table, final Column column) {
        try {
            final UpdateQuery updateQuery = UpdateQuery.get(table);

            updateQuery.addUpdateColumn(column, null);
            updateQuery.setWhereClause(new WhereClause(Criteria.eq(column, deletedId)));

            orgDataStore(orgId).update(updateQuery);
            LOGGER.debug("Cleaned up in table {} column {} for deleted ID {}", table.getName(), column.getColumnName(), deletedId);
        } catch (final Exception exception) {
            LOGGER.error("Error cleaning up single-value column: table={}, column={}, deletedId={}: {}", table.getName(), column.getColumnName(), deletedId, exception.getMessage(), exception);
        }
    }

    /**
     * <p>
     * Handles cleanup of multi-value columns where the deleted ID is part of a bigint array.
     * This method reads affected rows, removes the deleted ID from the array, and updates them.
     * </p>
     *
     * @param orgId     the organization ID
     * @param deletedId the deleted ID to remove from arrays
     * @param table     the table to update
     * @param column    the column containing bigint array data
     */
    private void cleanupMultiValueColumn(final long orgId, final long deletedId, final Table table, final Column column) {
        try {
            final DataContainer dataContainer = orgDataStore(orgId).get(table, new WhereClause(Criteria.in(column, List.of(deletedId))));
            final DataContainer updateContainer = DataContainer.create();

            dataContainer.getRows(table).forEach(row -> {
                final Object currentValue = row.get(column);

                if (Objects.nonNull(currentValue)) {
                    final List<Long> updatedArray = removeIdFromArray(currentValue, deletedId);

                    if (Objects.nonNull(updatedArray)) {
                        row.set(column, updatedArray.isEmpty() ? null : updatedArray);
                        updateContainer.updateRow(row);
                    }
                }
            });

            if (!updateContainer.isEmpty()) {
                orgDataStore(orgId).commitChanges(updateContainer);
            }
        } catch (final Exception exception) {
            LOGGER.error("Error cleaning up multi-value array matches: table={}, column={}, deletedId={}: {}", table.getName(), column.getColumnName(), deletedId, exception.getMessage(), exception);
        }
    }

    /**
     * <p>
     * Removes a specific ID from a bigint array.
     * </p>
     *
     * @param arrayValue the array value (can be Long[],or Collection)
     * @param idToRemove the ID to remove from the array
     * @return the updated array with the ID removed, or null if the ID was not found
     */
    private List<Long> removeIdFromArray(final Object arrayValue, final long idToRemove) {
        final List<Long> idList = convertToList(arrayValue);

        if (Objects.isNull(idList) || idList.isEmpty()) {
            return null;
        }

        if (idList.remove(idToRemove)) {
            return idList;
        }
        return null;
    }

    /**
     * <p>
     * Converts array value to a mutable List of Long.
     * Handles Long[], long[], and Collection types.
     * </p>
     *
     * @param arrayValue the array value to convert
     * @return a mutable list of Long values, or null if conversion fails
     */
    private List<Long> convertToList(final Object arrayValue) {
        if (arrayValue instanceof final Long[] longArray) {
            return new ArrayList<>(Arrays.asList(longArray));
        } else if (arrayValue instanceof final List<?> list) {
            return list.stream().map(value -> (Long) value).collect(Collectors.toList());
        }
        return null;
    }

    /**
     * <p>
     * Gets the organization-specific data store for executing queries.
     * </p>
     *
     * @param orgId Represents the organization ID
     * @return The {@link OrgDataStore} for the given organization
     */
    private static OrgDataStore orgDataStore(final long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }
}