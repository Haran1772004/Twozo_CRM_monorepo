package com.tlc.sql.olap.api.dml;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Operator;

import java.util.Collection;

/**
 * OLAP-specific criteria extending {@link Criteria}.
 * Provides factory methods for ClickHouse-specific operators such as {@code hasAny} and {@code notHasAny}.
 */
public class OlapCriteria extends Criteria {

    private OlapCriteria(Column column, Object value, Operator operator) {
        super(column, value, operator);
    }

    /**
     * Creates a {@link Criteria} using ClickHouse's {@code hasAny(column, [values])} function.
     * Matches rows where the array column contains any of the specified values.
     *
     * @param column the array {@link Column} to check
     * @param value  the collection of values to match against the array
     * @return a new OlapCriteria representing {@code hasAny(column, [values])}
     */
    public static OlapCriteria hasAny(final Column column, final Collection<?> value) {
        return new OlapCriteria(column, value, Operator.HAS_ANY);
    }

    /**
     * Creates a {@link Criteria} using ClickHouse's {@code NOT hasAny(column, [values])} function.
     * Matches rows where the array column does not contain any of the specified values.
     *
     * @param column the array {@link Column} to check
     * @param value  the collection of values to exclude from the array
     * @return a new OlapCriteria representing {@code NOT hasAny(column, [values])}
     */
    public static OlapCriteria notHasAny(final Column column, final Collection<?> value) {
        return new OlapCriteria(column, value, Operator.NOT_HAS_ANY);
    }
}
