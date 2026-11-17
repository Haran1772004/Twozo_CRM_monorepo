package com.tlc.analytics.config.model.view.report.chart;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.sql.resource.ANACHARTPROPERTY;
import com.tlc.analytics.sql.resource.ANAGRAPHSUBTYPE;
import com.tlc.analytics.sql.resource.ANAGRAPHTYPE;
import com.tlc.analytics.config.keys.view.report.chart.ChartTypeKeys;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Relation;
import com.tlc.entity.annotation.Table;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Collection;

/**
 * <p>
 * A model that manages the chart type details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANAGRAPHTYPE.TABLE )
public class ChartType extends BaseModel
{
    @JsonKey( key = ChartTypeKeys.TYPE )
    @Column( name = ANAGRAPHSUBTYPE.TYPE )
    @NotNull
    private String graphType;

    @JsonKey( key = ChartTypeKeys.IS_ENABLED )
    @Column( name = ANAGRAPHSUBTYPE.IS_ENABLED )
    @NotNull
    private Boolean isEnabled;

    @JsonKey( key = ChartTypeKeys.SUB_TYPES )
    @Join( relation = Relation.ONE_TO_ONE,
           joinTables = @JoinTable( joinColumns = @JoinColumn( referenceColumn = ANAGRAPHSUBTYPE.GRAPH_TYPE ),
                                    entityClass = ChartSubType.class ) )
    @Valid
    @NotNull
    private Collection<GraphSubType> graphSubTypes;

    @JsonKey( key = ChartTypeKeys.CHART_PROPERTIES )
    @Join( relation = Relation.ONE_TO_ONE,
           joinTables = @JoinTable( joinColumns = @JoinColumn( referenceColumn = ANACHARTPROPERTY.GRAPH_TYPE ),
                                    entityClass = ChartProperties.class ) )
    @Valid
    @NotNull
    private ChartProperties chartProps;
}
