package com.tlc.sql.common.internal.data;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.data.RowFilter;

import java.util.Objects;

/**
 * Implementation for filtering results retrieved from the database
 *
 * @author Abishek
 * @version 1.0
 */
public class DataContainerFilter extends RowFilter<Row> {

    private final WhereClause whereClause;

    DataContainerFilter(final WhereClause whereClause) {
        this.whereClause = Objects.requireNonNull(whereClause);
    }

    /**
     * Evaluates the row for the specified criteria
     *
     * @param row Represents the row to be tested
     * @return true if the row matches the provided criteria
     */
    @Override
    public boolean test(final Row row) {
        return match(whereClause, row);
    }

    /**
     * {@inheritDoc}
     *
     * @param row         The row containing the value to be retrieved
     * @param columnAlias Represents the column name to retrieve the value from
     * @return The value associated with the given column name
     */
    @Override
    protected Object getData(final Row row, final String columnAlias) {
        return row.get(columnAlias);
    }
}
