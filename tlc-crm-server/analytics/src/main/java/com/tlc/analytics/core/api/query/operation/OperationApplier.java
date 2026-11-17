package com.tlc.analytics.core.api.query.operation;

import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.sql.common.api.dml.Column;
import lombok.NonNull;

/**
 * Interface for applying operations to a column based on the operation type
 *
 * @author Aswath
 * @version 1.0
 */
public interface OperationApplier
{
    /**
     * Applies the specified operation to the given source column and returns the modified column.
     *
     * @param sourceColumn  The column to which the operation is to be applied.
     * @param operationType The type of operation to apply.
     * @return The column with the applied operation.
     */
    Column applyAndGet( final @NonNull Column sourceColumn, final @NonNull OperationType operationType );

    /**
     * Retrieves the {@link OperationApplier} instance for numerical fields.
     *
     * @return The {@link OperationApplier} instance for handling numerical field operations.
     */
    static OperationApplier getForNumericalField()
    {
        return Numerical.INSTANCE();
    }

    /**
     * Retrieves the {@link OperationApplier} instance for date fields.
     *
     * @return The {@link OperationApplier} instance for handling date field operations.
     */
    static OperationApplier getForDateField()
    {
        return Date.INSTANCE();
    }
}
