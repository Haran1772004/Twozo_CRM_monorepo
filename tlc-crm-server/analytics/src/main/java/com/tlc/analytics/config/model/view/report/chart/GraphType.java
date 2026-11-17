package com.tlc.analytics.config.model.view.report.chart;

import com.tlc.analytics.config.internal.ConfigErrorCodes;

import com.tlc.commons.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * Represents the graph types
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@RequiredArgsConstructor
@Getter
public enum GraphType
{
    BAR( 1, "barChart", "i18n.analytics.graph.type.bar" ),
    PIE( 2, "pieChart", "i18n.analytics.graph.type.pie" ),
    LINE( 3, "lineChart", "i18n.analytics.graph.type.line" ),
    KPI( 19,"kpiCard","i18n.analytics.graph.type.kpi" ),
    AREA( 5, "areaChart", "i18n.crm.analytics.graph.type.area.chart" ),
    DONUT( 6, "donutChart", "i18n.crm.analytics.graph.type.donut.chart" ),
    HORIZONTAL_BAR( 7, "horizontalBarChart", "i18n.crm.analytics.graph.type.horizontal.bar.chart" ),
    HEAT_MAP( 8, "heatMap", "i18n.crm.analytics.graph.type.heat.map" ),
    GROUPED_BAR( 9, "groupedBarChart", "i18n.crm.analytics.graph.type.grouped.bar.chart" ),
    GROUPED_HORIZONTAL_BAR( 10, "groupedHorizontalBarChart", "i18n.crm.analytics.graph.type.grouped.horizontal.bar.chart" ),
    STACKED_BAR( 11, "stackedBarChart", "i18n.crm.analytics.graph.type.stacked.bar.chart" ),
    STACKED_HORIZONTAL_BAR( 12, "stackedHorizontal", "i18n.crm.analytics.graph.type.stacked.horizontal.bar.chart" ),
    SUMMARY( 13, "summaryTable", "i18n.crm.analytics.graph.type.summary.table" ),
    BUBBLE( 14, "bubbleChart", "i18n.crm.analytics.graph.type.bubble.chart" );

    private final int typeCode;
    private final String aliasName;
    private final String i18nKeyOfDisplayName;

    /**
     * Retrieves the {@link GraphType} enum corresponding to a given type code.
     *
     * @param graphCode  The type code to look up.
     * @return The {@link GraphType} enum associated with the provided code.
     * @throws IllegalArgumentException if the provided code does not match any known operation type.
     */
    public static GraphType getGraphType( final int graphCode )
    {
        return switch (graphCode) {
            case 1 -> BAR;
            case 2 -> PIE;
            case 3 -> LINE;
            case 5 -> AREA;
            case 6 -> DONUT;
            case 7 -> HORIZONTAL_BAR;
            case 8 -> HEAT_MAP;
            case 9 -> GROUPED_BAR;
            case 10 -> GROUPED_HORIZONTAL_BAR;
            case 11 -> STACKED_BAR;
            case 12 -> STACKED_HORIZONTAL_BAR;
            case 13 -> SUMMARY;
            case 14 -> BUBBLE;
            case 19 -> KPI;
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_GRAPH_TYPE, "i18n.analytics.invalid.graph.type");
         };
    }
}
