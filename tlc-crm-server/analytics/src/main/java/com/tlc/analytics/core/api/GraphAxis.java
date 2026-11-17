package com.tlc.analytics.core.api;

import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.dynamic.underlying.AnalyticsLimit;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparatorSide;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparatorType;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an axis in a graph.
 *
 * @param <T> The type of value stored in the axis.
 *
 * @author Aswath
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor( staticName = "create" )
public class GraphAxis<T>
{

    /**
     * Creates graph axis with column name and value for lightweight axis projections.
     *
     * @param columnName Represents column name.
     * @param value Represents axis value.
     */
    public GraphAxis( final Object columnName, final Object value )
    {
        this.rowIdx = getRowIdx();
        this.reportField = getReportField();
        this.columnName = columnName;
        this.value = value;
    }

    private final Long rowIdx;
    /**
     * The report field associated with the axis.
     */
    @NonNull
    private final ReportField reportField;

    /**
     * Collection of axis values associated with the axis.
     */
    private final List<AxisValue<T>> axisValues = new ArrayList<>();

    /**
     * The report fields values associated with the view
     */
    @Setter
    private Object value;

    /**
     * The report fields columns associated with the view
     */
    private Object columnName;

    /**
     * The {@link Map} represents the key as column id and value as row value
     */
    private final Map<Long, Object> columnMap = new HashMap<>();

    /**
     * The {@link Map} represents the key as column name and value as row value
     */
    private final Map<String, Long> columnNameMap = new HashMap<>();

    /**
     * Maps each column ID to its corresponding field ID.
     */
    private final Map<Long, Long> columnToFieldIdMap = new HashMap<>();

    @Setter
    private OperationType operationType;

    @Setter
    private KPIComparatorSide comparatorSide;

    @Setter
    private Long totalRecords;

    @Setter
    private AnalyticsLimit analyticsLimit;

    @Setter
    private KPIComparatorType comparatorType;
}
