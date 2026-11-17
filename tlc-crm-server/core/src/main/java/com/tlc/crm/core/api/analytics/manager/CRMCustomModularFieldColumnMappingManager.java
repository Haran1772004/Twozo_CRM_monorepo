package com.tlc.crm.core.api.analytics.manager;

import com.tlc.crm.core.api.analytics.entity.integ.reports.CRMCustomModularFieldColumnMapping;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.CRMCUSTOMMODULARFIELDCOLUMNMAPPING;
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
 * Manages CRM Custom Modular Field Column Mapping
 * </p>
 *
 * @author karthick-v
 * @version 2.0
 */
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CRMCustomModularFieldColumnMappingManager extends CRMAbstractReportsManager<CRMCustomModularFieldColumnMapping>
{
    private static class Instance
    {
        private static final CRMCustomModularFieldColumnMappingManager INSTANCE = new CRMCustomModularFieldColumnMappingManager();
    }

    /**
     * Returns singleton instance.
     *
     * @return {@link CRMCustomModularFieldColumnMappingManager} singleton
     */
    public static CRMCustomModularFieldColumnMappingManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * Returns supported model class.
     *
     * @return custom modular-field column mapping model class
     */
    @NonNull
    @Override
    protected Class<CRMCustomModularFieldColumnMapping> getModelClass()
    {
        return CRMCustomModularFieldColumnMapping.class;
    }

    /**
     * Returns associated table metadata.
     *
     * @return SQL table metadata
     */
    @Override
    public Table getTable()
    {
        return Table.get(CRMCUSTOMMODULARFIELDCOLUMNMAPPING.TABLE);
    }

    /**
     * Fetches all {@link CRMCustomModularFieldColumnMapping} from the database.
     *
     * @return {@link Collection} of {@link CRMCustomModularFieldColumnMapping}, never null
     */
    public Collection<CRMCustomModularFieldColumnMapping> fetchAll() {
        final Table table = Table.get(CRMCUSTOMMODULARFIELDCOLUMNMAPPING.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(buildBaseQuery(null, null));
        final Collection<CRMCustomModularFieldColumnMapping> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            dataContainer.getRows(table).forEach(row -> result.add(buildColumnMapping(row)));
        }

        LOGGER.info("CRM Analytics : Fetched [{}] CRMCustomModularFieldColumnMapping from DB", result.size());
        return result;
    }

    /**
     * Fetches the {@link Collection} of {@link CRMCustomModularFieldColumnMapping} from the database for the given module ID.
     *
     * @param moduleId Represents the module identifier
     * @return {@link Collection} of {@link CRMCustomModularFieldColumnMapping}, never null
     */
    public Collection<CRMCustomModularFieldColumnMapping> fetchByModuleId(final Integer moduleId) {
        final Table table = Table.get(CRMCUSTOMMODULARFIELDCOLUMNMAPPING.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(
                buildBaseQuery(table.getColumn(CRMCUSTOMMODULARFIELDCOLUMNMAPPING.MODULE_ID), moduleId));
        final Collection<CRMCustomModularFieldColumnMapping> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            dataContainer.getRows(table).forEach(row -> result.add(buildColumnMapping(row)));
        }

        LOGGER.info("CRM Analytics : Fetched [{}] CRMCustomModularFieldColumnMapping from DB for moduleId [{}]", result.size(), moduleId);
        return result;
    }

    /**
     * Fetches a single {@link CRMCustomModularFieldColumnMapping} from the database for the given column mapping ID.
     *
     * @param columnMappingId Represents the column mapping identifier
     * @return {@link CRMCustomModularFieldColumnMapping} or null if not found
     */
    public CRMCustomModularFieldColumnMapping fetchByColumnMappingId(final Long columnMappingId) {
        final Table table = Table.get(CRMCUSTOMMODULARFIELDCOLUMNMAPPING.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(
                buildBaseQuery(table.getPKColumn(), columnMappingId));

        if (Objects.nonNull(dataContainer)) {
            final Row row = dataContainer.getRow(table);

            if (Objects.nonNull(row)) {
                return buildColumnMapping(row);
            }
        }

        LOGGER.info("CRM Analytics : No CRMCustomModularFieldColumnMapping found in DB for columnMappingId [{}]", columnMappingId);
        return null;
    }

    /**
     * <p>
     * Builds the base {@link SelectQuery} for the custom modular field column mapping table.
     * Optionally applies a where clause if filterColumn and filterValue are provided.
     * </p>
     *
     * @param filterColumn The column to filter on, or null for no filter
     * @param filterValue  The value to filter by, or null for no filter
     * @return The constructed {@link SelectQuery}
     */
    private SelectQuery buildBaseQuery(final Column filterColumn, final Object filterValue) {
        final Table table = Table.get(CRMCUSTOMMODULARFIELDCOLUMNMAPPING.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(table);

        if (Objects.nonNull(filterColumn) && Objects.nonNull(filterValue)) {
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(filterColumn, filterValue)));
        }

        return selectQuery;
    }

    /**
     * <p>
     * Builds a {@link CRMCustomModularFieldColumnMapping} instance from the given row data.
     * </p>
     *
     * @param row The database row containing column mapping data
     * @return The constructed {@link CRMCustomModularFieldColumnMapping}
     */
    private CRMCustomModularFieldColumnMapping buildColumnMapping(final Row row) {
        return CRMCustomModularFieldColumnMapping.builder()
                .id(row.getPKValue())
                .moduleId(row.get(CRMCUSTOMMODULARFIELDCOLUMNMAPPING.MODULE_ID))
                .fieldTypeId(row.get(CRMCUSTOMMODULARFIELDCOLUMNMAPPING.FIELD_TYPE_ID))
                .columnId(row.get(CRMCUSTOMMODULARFIELDCOLUMNMAPPING.COLUMN_ID))
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
