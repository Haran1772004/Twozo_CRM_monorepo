package com.tlc.analytics.core.api;

import com.tlc.analytics.config.model.view.report.dynamic.underlying.AnalyticsUnderlyingData;
import com.tlc.analytics.core.api.internal.DataViewRequest;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * Manages the data view request for the underlying data
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
@Getter
@SuperBuilder
public class UnderlyingDataRequest extends DataViewRequest<AnalyticsUnderlyingData>
{
    private Long orgId;
    private Long userId;
    private Integer moduleId;

    /**
     * The column ID of the KPI's duration (X_AXIS) field.
     * When the underlying data is accessed from a KPI, this holds the column ID
     * that the KPI uses for its time-based duration filter (e.g., CREATED_TIME, MODIFIED_TIME).
     * {@code null} when the parent view is a chart (no duration applies).
     */
    private Long durationColumnId;

    /**
     * Custom duration start time (epoch millis) for {@code OperationType.CUSTOM}.
     * Only used when the KPI's operationType is CUSTOM and from/to values are explicitly set.
     */
    private Long durationFrom;

    /**
     * Custom duration end time (epoch millis) for {@code OperationType.CUSTOM}.
     * Only used when the KPI's operationType is CUSTOM and from/to values are explicitly set.
     */
    private Long durationTo;
}