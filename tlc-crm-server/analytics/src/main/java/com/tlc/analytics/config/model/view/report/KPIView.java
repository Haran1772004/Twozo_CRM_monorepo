package com.tlc.analytics.config.model.view.report;

import com.tlc.analytics.config.keys.view.report.ReportKeys;
import com.tlc.analytics.config.model.view.report.chart.GraphType;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparator;
import com.tlc.analytics.sql.resource.ANAREPORT;
import com.tlc.analytics.sql.resource.ANAWIDGETCOMPARATOR;

import com.tlc.entity.annotation.Cascade;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Relation;

import com.tlc.validator.type.Group;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Collection;

/**
 * <p>
 * A model that manages the {@link KPIView}
 * </p>
 *
 * @author james
 * @version 1.0
 */
@Getter
@SuperBuilder
public class KPIView extends AnalyticsReport
{

    @JsonKey( key = ReportKeys.GRAPH_TYPE )
    @Column( name = ANAREPORT.GRAPH_TYPE )
    @Getter( AccessLevel.NONE )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.chart.view.invalid.graph.type" )
    private Integer graphType;

    @Join(  relation   = Relation.ONE_TO_MANY,
            cascades   = Cascade.ALL,
            nullable   = true,
            joinTables = @JoinTable( aliasName = "comparators",
                                     joinColumns = @JoinColumn( referenceColumn = ANAWIDGETCOMPARATOR.VIEW_ID ),
                                                                entityClass = KPIComparator.class ) )
    @Size( min = 1 )
    @Valid
    private Collection<KPIComparator> comparators;

    public GraphType getGraphType()
    {
        return GraphType.getGraphType( graphType );
    }
}
