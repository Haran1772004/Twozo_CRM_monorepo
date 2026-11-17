package com.tlc.analytics.config.action.validator.reports;

import com.tlc.analytics.config.action.validator.AnalyticsConfigValidator;
import com.tlc.analytics.config.model.table.AnalyticsDataType;
import com.tlc.analytics.config.model.view.OperationType;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Provides an abstract way to validate the operation
 * </p>
 *
 * @author Jamesh
 * @version 1.0
 * @see AnalyticsConfigValidator
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class OperationValidator
{
    private static class Instance
    {
        private static final OperationValidator INSTANCE = new OperationValidator();
    }

    public static OperationValidator INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Checks if a view operation is valid for a given data type and operation type
     * </p>
     *
     * @param analyticsDataType      the data type to check
     * @param operationType the operation type to check
     * @return true if the view operation is valid, false otherwise
     */
    public final boolean isValidViewOperation(final AnalyticsDataType analyticsDataType, final OperationType operationType )
    {
        final Optional<AnalyticsDataType> optionalAnalyticsDataType = Optional.ofNullable(analyticsDataType);

        switch ( operationType )
        {
            case SUM, MIN, MAX, AVG ->
            {
                return optionalAnalyticsDataType.filter(dataType ->
                        Objects.equals( AnalyticsDataType.NUMERICAL, dataType ) ).isPresent();
            }
            case ACTUAL, COUNT ->
            {
                return optionalAnalyticsDataType.filter(dataType -> dataType == AnalyticsDataType.STRING
                                || dataType == AnalyticsDataType.NUMERICAL || dataType == AnalyticsDataType.DATE ).isPresent();
            }
            case DAILY, WEEKLY, MONTHLY, YEARLY, PREVIOUS ->
            {
                return optionalAnalyticsDataType.filter(dataType -> Objects.equals( AnalyticsDataType.DATE, dataType ) )
                        .isPresent();
            }
        }
        return true;
    }

}
