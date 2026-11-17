package com.tlc.analytics.config.model.view.report;

import com.tlc.analytics.config.keys.view.report.ChartKeys;
import com.tlc.analytics.config.model.view.report.chart.GraphType;
import com.tlc.analytics.sql.resource.ANAREPORT;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.JsonKey;

import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.SuperBuilder;


/**
 * <p>
 * A model that manages the chart view details
 * </p>
 *
 * @author Jamesh
 * @version 1.0
 * @see ANAREPORT
 */
@SuperBuilder
@Getter
public class ChartView extends AnalyticsReport
{
    @JsonKey( key = ChartKeys.GRAPH_TYPE )
    @Column( name = ANAREPORT.GRAPH_TYPE )
    @Getter( AccessLevel.NONE )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.chart.view.invalid.graph.type" )
    private Integer graphType;

    public GraphType getGraphType()
    {
        return GraphType.getGraphType( graphType );
    }

    private Long chartUniqueId;
}
