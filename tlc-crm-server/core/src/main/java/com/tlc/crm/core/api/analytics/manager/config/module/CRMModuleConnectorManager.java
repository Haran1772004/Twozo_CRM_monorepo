package com.tlc.crm.core.api.analytics.manager.config.module;

import com.tlc.crm.core.api.analytics.entity.config.modules.CRMModuleConnector;
import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.sql.resource.CRMMODULECONNECTOR;

import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Provides an abstract way to manage the module column mapping
 * </p>
 *
 * @author karthick
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class CRMModuleConnectorManager extends CRMAbstractReportsManager<CRMModuleConnector>
{

    private static class Instance
    {
        private static final CRMModuleConnectorManager INSTANCE = new CRMModuleConnectorManager();
    }

    public static CRMModuleConnectorManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    protected @NonNull Class<CRMModuleConnector> getModelClass()
    {
        return CRMModuleConnector.class;
    }

    @Override
    public Table getTable()
    {
        return Table.get( CRMMODULECONNECTOR.TABLE );
    }

    /**
     * Fetches all {@link CRMModuleConnector} records from the database.
     *
     * @return {@link Collection} of {@link CRMModuleConnector}, never null
     */
    public Collection<CRMModuleConnector> fetchAll() {
        final Table connectorTable = Table.get(CRMMODULECONNECTOR.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(buildBaseQuery(null, null));
        final Collection<CRMModuleConnector> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            dataContainer.getRows(connectorTable).forEach(row -> result.add(buildModuleConnector(row)));
        }

        LOGGER.debug("CRM Analytics: Fetched {} module connectors from DB", result.size());
        return result;
    }

    /**
     * Fetches the {@link Collection} of {@link CRMModuleConnector} from the database for the given metrics module ID.
     *
     * @param metricsModuleId Represents the metrics module identifier
     * @return {@link Collection} of {@link CRMModuleConnector}, never null
     */
    public Collection<CRMModuleConnector> fetchByMetricsModuleId(final Integer metricsModuleId) {
        final Table connectorTable = Table.get(CRMMODULECONNECTOR.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(
                buildBaseQuery(connectorTable.getColumn(CRMMODULECONNECTOR.METRICS_MODULE_ID), metricsModuleId));
        final Collection<CRMModuleConnector> result = new ArrayList<>();

        if (Objects.nonNull(dataContainer)) {
            dataContainer.getRows(connectorTable).forEach(row -> result.add(buildModuleConnector(row)));
        }

        LOGGER.debug("CRM Analytics: Fetched {} module connectors from DB for metricsModuleId: {}", result.size(), metricsModuleId);
        return result;
    }

    /**
     * Fetches a single {@link CRMModuleConnector} from the database for the given connector ID.
     *
     * @param connectorId Represents the connector identifier
     * @return {@link CRMModuleConnector} or null if not found
     */
    public CRMModuleConnector fetchByConnectorId(final Long connectorId) {
        final Table connectorTable = Table.get(CRMMODULECONNECTOR.TABLE);
        final DataContainer dataContainer = getReadableDataStore().get(
                buildBaseQuery(connectorTable.getPKColumn(), connectorId));

        if (Objects.nonNull(dataContainer)) {
            final Row row = dataContainer.getRow(connectorTable);

            if (Objects.nonNull(row)) {
                final Integer metricsModuleId = row.get(CRMMODULECONNECTOR.METRICS_MODULE_ID);

                if (Objects.nonNull(metricsModuleId)) {
                    return buildModuleConnector(row);
                }
            }
        }

        LOGGER.debug("CRM Analytics: No module connector found in DB for connectorId: {}", connectorId);
        return null;
    }

    /**
     * <p>
     * Builds the base {@link SelectQuery} for the module connector table.
     * Optionally applies a where clause if filterColumn and filterValue are provided.
     * </p>
     *
     * @param filterColumn The column to filter on, or null for no filter
     * @param filterValue  The value to filter by, or null for no filter
     * @return The constructed {@link SelectQuery}
     */
    private SelectQuery buildBaseQuery(final Column filterColumn, final Object filterValue) {
        final Table table = Table.get(CRMMODULECONNECTOR.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(table);

        if (Objects.nonNull(filterColumn) && Objects.nonNull(filterValue)) {
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(filterColumn, filterValue)));
        }

        return selectQuery;
    }

    /**
     * <p>
     * Builds a {@link CRMModuleConnector} instance from the given row data.
     * </p>
     *
     * @param row The database row containing module connector data
     * @return The constructed {@link CRMModuleConnector}
     */
    private CRMModuleConnector buildModuleConnector(final Row row) {
        return CRMModuleConnector.builder()
                .id(row.getPKValue())
                .metricsModuleId(row.get(CRMMODULECONNECTOR.METRICS_MODULE_ID))
                .viewByModuleId(row.get(CRMMODULECONNECTOR.VIEW_BY_MODULE_ID))
                .columnId(row.get(CRMMODULECONNECTOR.COLUMN_ID))
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
