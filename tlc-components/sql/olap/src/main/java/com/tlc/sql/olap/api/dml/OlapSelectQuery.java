package com.tlc.sql.olap.api.dml;

import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.olap.internal.dml.OlapSelectQueryImpl;

/**
 * OLAP-specific extension of {@link SelectQuery}.
 * <p>
 * Provides methods specific to OLAP engines, such as requesting the latest version
 * of each record (e.g., ClickHouse's FINAL modifier for ReplacingMergeTree tables).
 * </p>
 */
public interface OlapSelectQuery extends SelectQuery {

    /**
     * Creates a new {@link OlapSelectQuery} for the given table with the latest record flag
     * defaulted to {@code true}.
     *
     * @param table the base table for the query
     * @return a new OlapSelectQuery instance
     */
    static OlapSelectQuery get(final Table table) {
        return new OlapSelectQueryImpl(table);
    }

    /**
     * Creates a new {@link OlapSelectQuery} for the given table.
     *
     * @param table               the base table for the query
     * @param returnUpdatedData {@code true} to return only the latest record version,
     *                            {@code false} otherwise
     * @return a new OlapSelectQuery instance
     */
    static OlapSelectQuery get(final Table table, final boolean returnUpdatedData) {
        return new OlapSelectQueryImpl(table, returnUpdatedData);
    }

    /**
     * Sets whether the query should return only the latest version of each record.
     * <p>
     * In OLAP engines that support versioned/replaceable rows (e.g., ClickHouse's
     * ReplacingMergeTree), enabling this ensures that only the most recent version
     * of each row is returned.
     * </p>
     *
     * @param returnUpdatedData {@code true} to return only the latest record version,
     *                            {@code false} otherwise
     */
    void returnUpdatedData(boolean returnUpdatedData);

    /**
     * Returns whether the query is configured to return only the latest version of each record.
     *
     * @return {@code true} if the latest record version should be returned, {@code false} otherwise
     */
    boolean isReturnUpdatedData();
}

