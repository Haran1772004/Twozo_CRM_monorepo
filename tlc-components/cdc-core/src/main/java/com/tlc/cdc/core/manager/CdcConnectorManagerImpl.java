package com.tlc.cdc.core.manager;

import com.tlc.cdc.core.models.ConnectorConfig;
import com.tlc.cdc.core.sql.resource.MCDCCONNECTORS;
import com.tlc.cdc.core.sql.resource.MCDCTABLES;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DmlUtil;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Manages the DB action of CDC connector details.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
final class CdcConnectorManagerImpl implements CdcConnectorManager {

    private final ReadableDataStore readableDataStore;

    private CdcConnectorManagerImpl() {
        this.readableDataStore = SqlServiceProvider.get().getAdminDataStore();
    }

    private static class Instance {
        private static final CdcConnectorManagerImpl INSTANCE = new CdcConnectorManagerImpl();
    }

    /**
     * Gets the {@link CdcConnectorManagerImpl} instance.
     *
     * @return {@link CdcConnectorManagerImpl}.
     */
    static CdcConnectorManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    private Table getTable() {
        return Table.get(MCDCCONNECTORS.TABLE);
    }

    /**
     * Gets the connector table.
     *
     * @return The connector table.
     */
    private Table getConnectorTable() {
        return Table.get(MCDCTABLES.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param type The connector type to filter by.
     */
    @Override
    public Collection<ConnectorConfig> getAll(final ConnectorConfig.Type type) {
        final Table table = getTable();
        final Table connectorTable = getConnectorTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addJoinClause(DmlUtil.getJoins(table, connectorTable));
        selectQuery.addSelectClause(table, connectorTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(MCDCCONNECTORS.TYPE), type.getId())));

        final DataContainer dataContainer = readableDataStore.get(selectQuery);

        return dataContainer.getRows(table).map(row -> {
            final Long pkValue = row.getPKValue();
            final List<String> connectorTables = dataContainer.getRows(connectorTable, new WhereClause(Criteria.eq(connectorTable.getColumn(MCDCTABLES.CONNECTOR_ID), pkValue)))
                    .map(connectorTableRow -> (String) connectorTableRow.get(MCDCTABLES.NAME)).toList();

            return new ConnectorConfig(pkValue, row.get(MCDCCONNECTORS.NAME), connectorTables, ConnectorConfig.Type.get(row.get(MCDCCONNECTORS.TYPE)));
        }).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param tableName The name of the source table to look up.
     * @return
     */
    public String getAliasName(final String tableName) {
        final Table table = getTable();
        final Table connectorTable = getConnectorTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addJoinClause(DmlUtil.getJoins(table, connectorTable));
        selectQuery.addSelectClause(List.of(table.getColumn(MCDCCONNECTORS.ALIAS_NAME)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(connectorTable.getColumn(MCDCTABLES.NAME), tableName)));

        final Row row = readableDataStore.get(selectQuery).getRow(table);

        return Objects.isNull(row) ? null : row.get(MCDCCONNECTORS.ALIAS_NAME);
    }
}
