package com.tlc.crm.core.api.analytics.manager;

import com.tlc.crm.core.api.analytics.entity.integ.reports.CRMSystemModularFieldColumnMapping;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.CRMSYSTEMMODULARFIELDCOLUMNMAPPING;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Manages CRM System Modular Field Column Mapping
 * </p>
 *
 * @author karthick-v
 * @version 2.0
 */
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CRMSystemModularFieldColumnMappingManager extends CRMAbstractReportsManager<CRMSystemModularFieldColumnMapping> {
    private static class Instance {
        private static final CRMSystemModularFieldColumnMappingManager INSTANCE = new CRMSystemModularFieldColumnMappingManager();
    }

    /**
     * Returns singleton instance.
     *
     * @return {@link CRMSystemModularFieldColumnMappingManager} singleton
     */
    public static CRMSystemModularFieldColumnMappingManager INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * Returns supported model class.
     *
     * @return system modular-field column mapping model class
     */
    @NonNull
    @Override
    protected Class<CRMSystemModularFieldColumnMapping> getModelClass() {
        return CRMSystemModularFieldColumnMapping.class;
    }

    /**
     * Returns associated table metadata.
     *
     * @return SQL table metadata
     */
    @Override
    public Table getTable() {
        return Table.get(CRMSYSTEMMODULARFIELDCOLUMNMAPPING.TABLE);
    }

    /**
     * Fetches all {@link CRMSystemModularFieldColumnMapping} from the database.
     *
     * @return {@link Collection} of {@link CRMSystemModularFieldColumnMapping}, never null
     */
    public Collection<CRMSystemModularFieldColumnMapping> fetchAll() {
        final Table table = Table.get(CRMSYSTEMMODULARFIELDCOLUMNMAPPING.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(buildBaseQuery(null, null));
        final Collection<CRMSystemModularFieldColumnMapping> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            dataContainer.getRows(table).forEach(row -> result.add(buildColumnMapping(row)));
        }

        LOGGER.info("CRM Analytics : Fetched [{}] CRMSystemModularFieldColumnMapping from DB", result.size());
        return result;
    }

    /**
     * Fetches the {@link Collection} of {@link CRMSystemModularFieldColumnMapping} from the database for the given modular field ID.
     *
     * @param modularFieldId Represents the modular field identifier
     * @return {@link Collection} of {@link CRMSystemModularFieldColumnMapping}, never null
     */
    public Collection<CRMSystemModularFieldColumnMapping> fetchByModularFieldId(final Long modularFieldId) {
        final Table table = Table.get(CRMSYSTEMMODULARFIELDCOLUMNMAPPING.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(
                buildBaseQuery(table.getColumn(CRMSYSTEMMODULARFIELDCOLUMNMAPPING.MODULAR_FIELD_ID), modularFieldId));
        final Collection<CRMSystemModularFieldColumnMapping> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            dataContainer.getRows(table).forEach(row -> result.add(buildColumnMapping(row)));
        }

        LOGGER.info("CRM Analytics : Fetched [{}] CRMSystemModularFieldColumnMapping from DB for modularFieldId [{}]", result.size(), modularFieldId);
        return result;
    }

    /**
     * Fetches a single {@link CRMSystemModularFieldColumnMapping} from the database for the given mapping ID.
     *
     * @param mappingId Represents the mapping identifier
     * @return {@link CRMSystemModularFieldColumnMapping} or null if not found
     */
    public CRMSystemModularFieldColumnMapping fetchByMappingId(final Long mappingId) {
        final Table table = Table.get(CRMSYSTEMMODULARFIELDCOLUMNMAPPING.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(
                buildBaseQuery(table.getPKColumn(), mappingId));

        if (Objects.nonNull(dataContainer)) {
            final Row row = dataContainer.getRow(table);

            if (Objects.nonNull(row)) {
                return buildColumnMapping(row);
            }
        }

        LOGGER.info("CRM Analytics : No CRMSystemModularFieldColumnMapping found in DB for mappingId [{}]", mappingId);
        return null;
    }

    /**
     * <p>
     * Builds the base {@link SelectQuery} for the system modular field column mapping table.
     * Optionally applies a where clause if filterColumn and filterValue are provided.
     * </p>
     *
     * @param filterColumn The column to filter on, or null for no filter
     * @param filterValue  The value to filter by, or null for no filter
     * @return The constructed {@link SelectQuery}
     */
    private SelectQuery buildBaseQuery(final Column filterColumn, final Object filterValue) {
        final Table table = Table.get(CRMSYSTEMMODULARFIELDCOLUMNMAPPING.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(table);

        if (Objects.nonNull(filterColumn) && Objects.nonNull(filterValue)) {
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(filterColumn, filterValue)));
        }

        return selectQuery;
    }

    /**
     * <p>
     * Builds a {@link CRMSystemModularFieldColumnMapping} instance from the given row data.
     * </p>
     *
     * @param row The database row containing column mapping data
     * @return The constructed {@link CRMSystemModularFieldColumnMapping}
     */
    private CRMSystemModularFieldColumnMapping buildColumnMapping(final Row row) {
        return CRMSystemModularFieldColumnMapping.builder()
                .id(row.getPKValue())
                .modularFieldId(row.get(CRMSYSTEMMODULARFIELDCOLUMNMAPPING.MODULAR_FIELD_ID))
                .joinFieldId(row.get(CRMSYSTEMMODULARFIELDCOLUMNMAPPING.JOIN_FIELD_ID))
                .selectorFieldId(row.get(CRMSYSTEMMODULARFIELDCOLUMNMAPPING.SELECTOR_FIELD_ID))
                .fieldCategory(row.get(CRMSYSTEMMODULARFIELDCOLUMNMAPPING.FIELD_CATEGORY))
                .havingOperationType(row.get(CRMSYSTEMMODULARFIELDCOLUMNMAPPING.HAVING_OPERATION_TYPE))
                .caseWhenConfig(row.get(CRMSYSTEMMODULARFIELDCOLUMNMAPPING.CASE_WHEN_CONFIG))
                .filterFieldId(row.get(CRMSYSTEMMODULARFIELDCOLUMNMAPPING.FILTER_FIELD_ID))
                .filterValue(row.get(CRMSYSTEMMODULARFIELDCOLUMNMAPPING.FILTER_VALUE))
                .build();
    }

    /**
     * Returns the {@link ReadableDataStore} instance for executing read queries.
     *
     * @return {@link ReadableDataStore}
     */
    private ReadableDataStore getReadableDataStore() {
        return DataStoreProvider.getReadableDataStore();
    }
}
