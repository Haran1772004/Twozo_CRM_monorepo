package com.tlc.analytics.config.model.view.report.chart;

import com.tlc.analytics.sql.resource.ANACHARTPROPERTY;
import com.tlc.analytics.config.keys.view.report.chart.ChartPropertiesKeys;
import com.tlc.analytics.config.model.BaseModel;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Table;

import jakarta.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.SuperBuilder;


/**
 * <p>
 * A model that manages the char properties details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANACHARTPROPERTY.TABLE )
public class ChartProperties extends BaseModel
{
    @JsonKey( key = ChartPropertiesKeys.GRAPH_TYPE )
    @Getter( AccessLevel.NONE )
    @Column( name = ANACHARTPROPERTY.GRAPH_TYPE )
    @NotNull
    Integer graphType;

    @JsonKey( key = ChartPropertiesKeys.HAS_LEGEND )
    @Column( name = ANACHARTPROPERTY.HAS_LEGEND )
    @NotNull
    Boolean hasLegend;

    @JsonKey( key = ChartPropertiesKeys.HAS_AXIS )
    @Column( name = ANACHARTPROPERTY.HAS_AXIS )
    @NotNull
    Boolean hasAxis;

    public GraphType getGraphType()
    {
        return GraphType.getGraphType( graphType );
    }
}
