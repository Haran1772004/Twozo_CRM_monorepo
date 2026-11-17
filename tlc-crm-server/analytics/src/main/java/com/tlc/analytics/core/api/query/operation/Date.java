package com.tlc.analytics.core.api.query.operation;

import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.sql.common.api.dml.Column;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Implementation of the {@link OperationApplier} interface for date columns.
 *
 * @author Aswath
 * @version 1.0
 */
@NoArgsConstructor( staticName = "create", access = AccessLevel.PRIVATE )
class Date implements OperationApplier
{
    private static class Instance
    {
        private static final Date INSTANCE = new Date();
    }

    public static Date INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceColumn  The column to which the operation is to be applied.
     * @param operationType The type of operation to apply.
     * @return The column with the applied operation. If the operation is not supported, returns null.
     */
    @Override
    public Column applyAndGet( final @NonNull Column sourceColumn, final @NonNull OperationType operationType )
    {
        return switch ( operationType )
        {
            case DAILY, WEEKLY, MONTHLY, YEARLY, PREVIOUS, TODAY, THIS_WEEK, THIS_MONTH, THIS_QUARTER, THIS_YEAR, CUSTOM -> sourceColumn;
            case MIN -> sourceColumn.min();
            case MAX -> sourceColumn.max();
            case SUM -> sourceColumn.sum();
            default -> null;
        };
    }
}
