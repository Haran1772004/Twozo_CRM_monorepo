package com.tlc.analytics.config.model.view.report.chart;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.config.keys.view.report.chart.ChartSubTypeKeys;
import com.tlc.analytics.sql.resource.ANAGRAPHSUBTYPE;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Table;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the chart sub type details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANAGRAPHSUBTYPE.TABLE )
public class ChartSubType extends BaseModel
{
    @JsonKey( key = ChartSubTypeKeys.TYPE )
    @Column( name = ANAGRAPHSUBTYPE.GRAPH_TYPE )
    @NotNull
    private Long graphType;

    @JsonKey( key = ChartSubTypeKeys.SUB_TYPE )
    @Column( name = ANAGRAPHSUBTYPE.TYPE )
    @NotNull
    private String graphSubType;

    @JsonKey( key = ChartSubTypeKeys.IS_ENABLED )
    @Column( name = ANAGRAPHSUBTYPE.IS_ENABLED )
    @NotNull
    private Boolean isEnabled;
}
