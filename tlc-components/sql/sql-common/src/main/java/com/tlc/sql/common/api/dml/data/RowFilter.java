package com.tlc.sql.common.api.dml.data;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.internal.status.SQLErrorCodes;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * Abstract class for filtering rows retrieved from the database result sets
 *
 * @param <T> Represents the type of rows from the result set
 * @author sundar
 * @version 1.0
 */
public abstract class RowFilter<T> implements Predicate<T> {

    /**
     * Checks whether the provided row matches the specified criteria
     *
     * @param whereClause Contains the criteria used to filter the row
     * @param row         The row to be evaluated
     * @return true if the row meets the given criteria
     */
    protected final boolean match(final WhereClause whereClause, final T row) {
        final boolean result;

        if (whereClause.isConnector()) {
            final WhereClause left = whereClause.getLeft();
            final WhereClause right = whereClause.getRight();
            final Connector connector = whereClause.getConnector();
            result = Connector.AND == connector ? match(left, row) && match(right, row) : match(left, row) || match(right, row);
        } else {
            final Criteria sqlCriteria = whereClause.getCriteria();
            final Column column = sqlCriteria.getColumn();
            final Object criData = sqlCriteria.getValue();
            final Operator operator = sqlCriteria.getOperator();

            final Object data = getData(row, column.getColumnAlias());
            final DataType dataType = column.getColumnDefinition().getDataType();

            result = switch (operator) {
                case EQUAL -> dataType.matches(data, criData);
                case NOT_EQUAL -> !dataType.matches(data, criData);
                case IN, NOT_IN -> {

                    if (data == null) {
                        yield false;
                    }

                    if (!(criData instanceof Collection)) {
                        throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_CRITERIA_INPUT);
                    }
                    yield operator == Operator.IN ? ((Collection<?>) criData).stream().anyMatch(s -> dataType.matches(s, data))
                            : ((Collection<?>) criData).stream().noneMatch(s -> dataType.matches(s, data));
                }
                default -> throw ErrorCode.get(ErrorCodes.NOT_SUPPORTED);
            };
        }
        return whereClause.isInverted() != result;
    }

    /**
     * Gets the value of the specified column from the provided row
     *
     * @param row         The row containing the value to be retrieved
     * @param columnAlias Represents the column name to retrieve the value from
     * @return The value associated with the given column name
     */
    protected abstract Object getData(final T row, final String columnAlias);

}
