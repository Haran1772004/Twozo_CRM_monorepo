package com.tlc.analytics.core.api.internal.general.view.response.graph;

import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.service.column.ColumnDetailsService;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.chart.GraphType;
import com.tlc.analytics.core.api.internal.request.dynamic.drillDown.DrillDownInfo;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * Handles the metadata for graph configurations, including axis and drill-down details
 * </p>
 *
 * @author Jamesh
 * @version 1.0
 */
@Accessors( fluent = true )
@NoArgsConstructor( staticName = "create", access = AccessLevel.MODULE )
class GraphMetaJson
{

    /**
     * Sets the metadata for a given axis based on the report field and organization ID.
     *
     * @param orgId    The organization ID to retrieve column details.
     * @param field    The report field containing axis and column information.
     * @param axisMeta The JsonObject to populate with axis metadata.
     * @throws NullPointerException if any of the parameters are null.
     */
    private void setAxisMeta( final @NonNull Long orgId, final @NonNull ReportField field, final JsonObject axisMeta )
    {
       final ColumnDetail columnDetail = ColumnDetailsService.getInstance().get( field.getColumnId() );
       final JsonObject columnMeta       = Json.object();

       assert columnDetail != null;
       columnMeta.put("columnId",       columnDetail.id() );
       columnMeta.put("columnName",     columnDetail.getColumnName() );
       columnMeta.put( "chartDataType", columnDetail.getDataType() );
       columnMeta.put( "actualDataType","" ); // Need to set the Actual AnalyticsDataType for UI purpose

       axisMeta.put("axis",        field.getAxis().getType() ); // Need to set the I18Key Value
       axisMeta.put( "label",      field.getLabel() );
       axisMeta.put( "columnMeta", columnMeta );
       axisMeta.put("operation",   field.getOperationType().getI18nKeyOfDisplayName() );
    }

    /**
     * Sets the metadata for drill-down information.
     *
     * @param drillDownInfo The drill-down information containing path and details.
     * @param drillDownMeta The JsonObject to populate with drill-down metadata.
     * @throws NullPointerException if any of the parameters are null.
     */
    private void setDrillDownMeta( final @NonNull DrillDownInfo drillDownInfo, final JsonObject drillDownMeta )
    {}

    /**
     * Sets the overall metadata JSON for the graph, including axis details and optional drill-down information.
     *
     * @param orgId              The organization ID used to fetch column details.
     * @param graphType          The type of the graph (e.g., bar, line, pie).
     * @param graphAxis          List of GraphAxis objects representing the axes of the graph.
     * @param isDrillDownApplied Boolean indicating if drill-down functionality is applied.
     * @param drillDownInfo      Optional DrillDownInfo containing drill-down details.
     * @param graphResponse      The JsonObject to populate with graph metadata.
     * @throws NullPointerException if any of the required parameters are null.
     */
    public void setMetaJson( final @NonNull Long orgId, final @NonNull GraphType graphType,
                             final @NonNull List<GraphAxis<Object>> graphAxis, final boolean isDrillDownApplied,
                             final DrillDownInfo drillDownInfo, final JsonObject graphResponse )
    {
        JsonObject graphMeta      = Json.object();
        final JsonArray axisArray = Json.array();
        JsonObject drillDownMeta  = Json.object();

        graphMeta.put( "graphType", graphType.getI18nKeyOfDisplayName() );

        for( GraphAxis<Object> axis : graphAxis )
        {
            final JsonObject axisMeta = Json.object();
            final JsonObject xAxisMeta = Json.object();
            setAxisMeta( orgId, axis.getReportField(), axisMeta );

            xAxisMeta.put( axis.getReportField()
                               .getAxis()
                               .name()
                               .toLowerCase(),
                           axisMeta );
            axisArray.put( xAxisMeta );
        }

        graphMeta.put("axisDetails", axisArray );

        if( isDrillDownApplied )
        {
            setDrillDownMeta( drillDownInfo, drillDownMeta );
            graphMeta.put( "drillDownDetails", drillDownMeta );
        }

        graphResponse.put("graphMeta", graphMeta );
    }
}
