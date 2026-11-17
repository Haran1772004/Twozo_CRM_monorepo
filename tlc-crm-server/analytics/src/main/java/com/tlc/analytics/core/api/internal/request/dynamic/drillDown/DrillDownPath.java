package com.tlc.analytics.core.api.internal.request.dynamic.drillDown;

import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.view.OperationType;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * <p>
 *     Represents the details of a drill-down path for a specific column and operation
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@Getter
@Builder
public class DrillDownPath
{
    private @NonNull ColumnDetail columnDetail;

    private @NonNull OperationType operationType;

    private @NonNull Object drillDownActualValue;

    private @NonNull Object drillDownChartValue;
}
