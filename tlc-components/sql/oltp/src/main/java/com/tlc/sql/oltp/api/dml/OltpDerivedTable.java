package com.tlc.sql.oltp.api.dml;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.internal.status.SQLErrorCodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Sundar
 * @version 1.0
 *
 * Represents a derived table in the OLTP module, extending the functionality of OltpTable.
 * This class provides methods to handle derived tables and their associated select queries.
 */
public class OltpDerivedTable extends OltpTable implements DerivedTable {

    private final SelectQuery selectQuery;
    private boolean isWithClauseCTE;

    /**
     * Lazy O(1) lookup index for the SELECT clause. Each entry's
     * {@code columnAlias} and {@code columnName} are both registered (alias
     * first, name second) using {@code putIfAbsent}, so the first column in
     * iteration order wins for any given key — preserving the exact
     * semantics of the previous {@code stream().filter(...).findFirst()}
     * implementation while turning per-call O(n) scans into O(1) lookups.
     */
    private Map<String, Column> selectClauseIndex;

    /**
     * Lazy, distinct list of {@link OltpDerivedTable} sources referenced by
     * the SELECT clause, used by {@link #findInDerivedSources}. Precomputed
     * so we avoid rebuilding a stream / casting pipeline on every miss.
     */
    private List<OltpDerivedTable> derivedSources;

    /**
     * Snapshot of {@code selectClause.size()} captured when the index and
     * derived-sources list were last built. A mismatch signals that the
     * SELECT clause has been mutated (added/removed entries) and the cached
     * structures must be rebuilt before the next lookup.
     */
    private int lastIndexedSize = -1;

    /**
     * Private constructor for creating an instance of OltpDerivedTable.
     *
     * @param table       the name of the table
     * @param alias       the alias for the table
     * @param selectQuery the select query associated with the derived table
     */
    private OltpDerivedTable(final String table, final String alias, final SelectQuery selectQuery) {
        super(table, alias);
        this.selectQuery = Objects.requireNonNull(selectQuery);
    }

    /**
     * Factory method to create a new instance of OltpDerivedTable.
     *
     * @param table       the name of the table
     * @param alias       the alias for the table
     * @param selectQuery the select query associated with the derived table
     * @return a new instance of DerivedTable
     */
    public static DerivedTable get(final String table, final String alias, final SelectQuery selectQuery) {
        return new OltpDerivedTable(table, alias, selectQuery);
    }

    /**
     * Retrieves the select query associated with the derived table.
     *
     * @return the select query
     */
    public SelectQuery getSelectQuery() {
        return selectQuery;
    }

    /**
     * Checks if the derived table is a Common Table Expression (CTE) in the WITH clause.
     *
     * @return true if the derived table is a CTE, false otherwise
     */
    @Override
    public boolean isWithClauseCTE() {
        return isWithClauseCTE;
    }

    /**
     * Sets whether the derived table is a Common Table Expression (CTE) in the WITH clause.
     *
     * @param withClauseCTE true to mark the derived table as a CTE, false otherwise
     */
    @Override
    public void setWithClauseCTE(final boolean withClauseCTE) {
        this.isWithClauseCTE = withClauseCTE;
    }

    /**
     * Retrieves a column from the derived table by its name.
     *
     * @param columnName the name of the column
     * @return the column object
     * @throws ErrorCode if the column is not found
     */
    @Override
    public Column getColumn(final String columnName) {
        try {
            return super.getColumn(columnName);
        } catch (final ErrorCode exception) {
            if (!isUnknownColumn(exception)) {
                throw ErrorCode.get(exception.getProvider(), columnName, exception);
            }

            final Column source = selectClauseIndex().get(columnName);

            if (source == null) {
                throw ErrorCode.get(SQLErrorCodes.DB_ROW_UNKNOWN_COLUMN, columnName);
            }

            return wrapColumn(source, columnName, null);
        }
    }

    /**
     * Retrieves a column from the derived table by its name and alias.
     *
     * @param columnName  the name of the column
     * @param columnAlias the alias of the column
     * @return the column object
     * @throws ErrorCode if the column is not found
     */
    @Override
    public Column getColumn(final String columnName, final String columnAlias) {
        try {
            return super.getColumn(columnName, columnAlias);
        } catch (final ErrorCode exception) {

            if (!isUnknownColumn(exception)) {
                throw ErrorCode.get(exception.getProvider(), columnName, exception);
            }

            return findInSelectClause(columnName, columnAlias)
                    .or(() -> findInDerivedSources(columnName, columnAlias))
                    .orElseThrow(() -> ErrorCode.get(SQLErrorCodes.DB_ROW_UNKNOWN_COLUMN, columnName));
        }
    }

    /**
     * Returns {@code true} if the given {@link ErrorCode} represents the
     * "unknown column" condition that the derived-table fallback path is
     * designed to recover from. Any other error code should propagate
     * unchanged so that real failures are not silently masked.
     *
     * @param errorCode the error code to inspect
     * @return whether the error code denotes an unknown column
     */
    private static boolean isUnknownColumn(final ErrorCode errorCode) {
        return errorCode.getProvider() == SQLErrorCodes.DB_ROW_UNKNOWN_COLUMN;
    }

    /**
     * O(1) lookup against the lazily-built SELECT clause index.
     */
    private Optional<Column> findInSelectClause(final String columnName, final String columnAlias) {
        final Column source = selectClauseIndex().get(columnName);

        return source == null ? Optional.empty() : Optional.of(wrapColumn(source, columnName, columnAlias));
    }

    /**
     * Iterates the precomputed derived sources directly instead of
     * rebuilding a stream/filter/cast pipeline on every call.
     */
    private Optional<Column> findInDerivedSources(final String columnName, final String columnAlias) {
        for (final OltpDerivedTable derivedSource : derivedSources()) {
            try {
                return Optional.of(wrapColumn(derivedSource.getColumn(columnName), columnName, columnAlias));
            } catch (final ErrorCode ex) {
                if (!isUnknownColumn(ex)) {
                    throw ErrorCode.get(ex.getProvider(), columnName, ex);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the lazy SELECT-clause lookup index, rebuilding it (along with
     * {@link #derivedSources}) if the SELECT clause size has changed.
     */
    private Map<String, Column> selectClauseIndex() {
        ensureIndexes();
        return selectClauseIndex;
    }

    /**
     * Returns the lazy distinct list of derived-table sources referenced by
     * the SELECT clause, rebuilding the caches if necessary.
     */
    private List<OltpDerivedTable> derivedSources() {
        ensureIndexes();
        return derivedSources;
    }

    /**
     * Builds (or rebuilds) {@link #selectClauseIndex} and
     * {@link #derivedSources} when first accessed or after a SELECT-clause
     * size change. The two structures are populated in a single pass over
     * the SELECT clause to amortise the cost.
     */
    private void ensureIndexes() {
        final Collection<? extends Column> selectClause = selectQuery.getSelectClause();
        final int currentSize = selectClause.size();

        if (selectClauseIndex != null && currentSize == lastIndexedSize) {
            return;
        }

        final Map<String, Column> index = new HashMap<>(Math.max(16, (currentSize * 2 * 4 / 3) + 1));
        final Map<OltpDerivedTable, Boolean> seenSources = new IdentityHashMap<>();
        final List<OltpDerivedTable> sources = new ArrayList<>();

        for (final Column column : selectClause) {
            final String alias = column.getColumnAlias();

            if (alias != null) {
                index.putIfAbsent(alias, column);
            }

            final String name = column.getColumnName();

            if (name != null) {
                index.putIfAbsent(name, column);
            }

            if (column.getTable() instanceof final OltpDerivedTable derivedSource
                    && seenSources.putIfAbsent(derivedSource, Boolean.TRUE) == null) {
                sources.add(derivedSource);
            }
        }

        this.selectClauseIndex = index;
        this.derivedSources = sources;
        this.lastIndexedSize = currentSize;
    }

    /**
     * Wraps a source column with the given column name and alias, preserving the
     * original OLTP behaviour of resolving the column through a freshly constructed
     * {@link OltpTable} that combines the source column's table name with this
     * derived table's alias.
     *
     * @param source      the original column to wrap
     * @param columnName  the name to assign to the wrapped column
     * @param columnAlias the alias to assign to the wrapped column, or {@code null}
     *                    to use the single-argument {@code getColumn} overload
     * @return a new {@link Column} instance bound to a table with this derived
     *         table's alias
     */
    private Column wrapColumn(final Column source, final String columnName, final String columnAlias) {
        final Table table = new OltpTable(source.getTable().getName(), getAlias());
        return columnAlias == null ? table.getColumn(columnName) : table.getColumn(columnName, columnAlias);
    }
}

