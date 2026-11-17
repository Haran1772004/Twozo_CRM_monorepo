package com.tlc.analytics.core.api.query.operation;

import com.tlc.analytics.config.model.view.OperationType;

import com.tlc.sql.common.api.dml.Column;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Implementation of the {@link OperationApplier} interface for numerical columns
 *
 * @author Aswath
 * @version 1.0
 */
@NoArgsConstructor( staticName = "create", access = AccessLevel.PRIVATE )
class Numerical implements OperationApplier
{
    private static class Instance
    {
        private static final Numerical INSTANCE = new Numerical();
    }

    public static Numerical INSTANCE()
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
        switch( operationType )
        {
            case SUM ->
            {
                return sourceColumn.sum();
            }
            case MAX ->
            {
                return sourceColumn.max();
            }
            case MIN ->
            {
                return sourceColumn.min();
            }
            case AVG ->
            {
                return sourceColumn.average();
            }
            case COUNT, TOTAL ->
            {
                return sourceColumn.count();
            }
            default ->
            {
                return null;
            }
        }
    }
}
