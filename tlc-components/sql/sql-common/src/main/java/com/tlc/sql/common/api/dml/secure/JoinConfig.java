package com.tlc.sql.common.api.dml.secure;

import com.tlc.commons.json.JsonObject;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.Table;

import java.util.Objects;

/**
 * Represents a configuration for joining two tables using a {@link JoinClause}.
 * This record encapsulates local and remote join column details and the join type.
 *
 * <p>This configuration can be used to generate a {@link JoinClause} object dynamically
 * using JSON configuration.</p>
 *
 * @author Sathish
 * @version 1.0
 */
public record JoinConfig(JsonObject local, JsonObject remote, JoinClause.JoinType joinType) {

    private static final String LOCAL = "local";
    private static final String REMOTE = "remote";
    private static final String COLUMN = "column";
    private static final String TABLE = "table";
    private static final String JOIN_TYPE = "joinType";

    public JoinConfig(final JsonObject joinConfig) {
        this(
                joinConfig.getJsonObject(LOCAL),
                joinConfig.getJsonObject(REMOTE),
                JoinClause.JoinType.getJoinType(joinConfig.getString(JOIN_TYPE))
        );
    }

    /**
     * Returns the type of join to be applied between tables.
     *
     * @return the join type
     */
    public JoinClause.JoinType getJoinType() {
        return joinType;
    }

    /**
     * Returns the {@link Column} object representing the local table's join column.
     *
     * @return the local join column
     */
    public Column getLocalColumn() {
        return buildColumn(local);
    }

    /**
     * Returns the {@link Column} object representing the remote table's join column.
     *
     * @return the remote join column
     */
    public Column getRemoteColumn() {
        return buildColumn(remote);
    }

    /**
     * Builds a {@link JoinClause} using the provided table definitions.
     * If no tables are passed, uses default table references from the JSON configuration.
     *
     * @param needJoinInversion  indicates whether the join type should be inverted (e.g., LEFT to RIGHT)
     * @param criteriaConnection the criteria connection to use for resolving column references
     * @param tables             optional table definitions that override default tables
     * @return the constructed join clause
     */
    public JoinClause getJoin(final boolean needJoinInversion, final Connector criteriaConnection, final Table... tables) {
        final JoinClause.JoinType joinType = getJoinType(criteriaConnection);

        if (0 < tables.length) {
            final String localTableName = local.getString(TABLE);
            final String remoteTableName = remote.getString(TABLE);
            Table localTable = Table.get(localTableName);
            Table remoteTable = Table.get(remoteTableName);

            for (final Table table : tables) {
                final String tableName = table.getName();

                if (Objects.equals(localTableName, tableName)) {
                    localTable = table;
                } else if (Objects.equals(remoteTableName, tableName)) {
                    remoteTable = table;
                }
            }

            final Column localColumn = buildColumn(localTable, local.getString(COLUMN));
            final Column remoteColumn = buildColumn(remoteTable, remote.getString(COLUMN));

            return needJoinInversion ? new JoinClause(remoteColumn, localColumn, joinType) : new JoinClause(localColumn, remoteColumn, joinType);
        }
        final Column localColumn = buildColumn(local);
        final Column remoteColumn = buildColumn(remote);

        return needJoinInversion ? new JoinClause(remoteColumn, localColumn, joinType) : new JoinClause(localColumn, remoteColumn, joinType);
    }

    /**
     * Determines the join type based on the provided criteria connection.
     * This method can be extended to implement logic that maps specific criteria connections to join types.
     *
     * @param criteriaConnection the criteria connection used to determine the join type
     * @return the corresponding {@link JoinClause.JoinType}, or null if no mapping is defined
     */
    private JoinClause.JoinType getJoinType(final Connector criteriaConnection) {
        return Objects.isNull(criteriaConnection)
                ? joinType : Connector.AND == criteriaConnection
                             ? JoinClause.JoinType.INNER : JoinClause.JoinType.LEFT;
    }

    /**
     * Builds a column from the provided JSON configuration using the default table.
     *
     * @param joinConfig JSON containing table and column information
     * @return the resolved {@link Column} object
     */
    private Column buildColumn(final JsonObject joinConfig) {
        return buildColumn(Table.get(joinConfig.getString(TABLE)), joinConfig.getString(COLUMN));
    }

    /**
     * Retrieves the column from the specified table.
     *
     * @param table      the table instance
     * @param columnName the name of the column to retrieve
     * @return the column from the given table
     */
    private Column buildColumn(final Table table, final String columnName) {
        return table.getColumn(columnName);
    }
}