package com.tlc.analytics.config.model.view.report;

import com.tlc.analytics.config.model.view.report.path.ReportPathMapping;
import com.tlc.analytics.config.model.view.ViewModel;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.keys.view.report.ReportKeys;
import com.tlc.analytics.sql.resource.ANAFILTERFIELD;
import com.tlc.analytics.sql.resource.ANAREPORT;
import com.tlc.analytics.sql.resource.ANAREPORTFIELD;

import com.tlc.entity.annotation.Cascade;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.Relation;
import com.tlc.entity.annotation.Table;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.JoinTable;

import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Collection;

/**
 * <p>
 * A model that manages the report details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANAREPORT.TABLE )
public class AnalyticsReport extends ViewModel
{
    @JsonKey( key = ReportKeys.ROOT_TABLE_ID )
    @Column( name = ANAREPORT.ROOT_TABLE )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.report.invalid.root.table.id" )
    private Long rootTableId;

    @JsonKey( key = ReportKeys.TITLE )
    @Column( name = ANAREPORT.TITLE )
    @NotEmpty( groups = { Group.Create.class },
               message = "i18n.analytics.report.invalid.title" )
    @Size( max = 50 )
    private String title;

    @JsonKey( key = ReportKeys.REPORT_FIELDS )
    @Join( relation = Relation.ONE_TO_MANY, cascades =
           Cascade.ALL,
           nullable = true,
           joinTables = @JoinTable( aliasName = "reportFields",
                                    joinColumns = @JoinColumn( referenceColumn = ANAREPORTFIELD.VIEW_ID ),
                                    entityClass = ReportField.class ) )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.report.invalid.report.field" )
    @Size( min = 1 )
    @Valid
    private Collection<ReportField> reportFields;

    @JsonKey( key = ReportKeys.FILTER_FIELDS )
    @Join( relation = Relation.ONE_TO_MANY,
           cascades = Cascade.ALL,
           nullable = true,
           joinTables = @JoinTable( aliasName = "filterFields",
                                    joinColumns = @JoinColumn( referenceColumn = ANAFILTERFIELD.VIEW_ID ),
                                    entityClass = FilterField.class ) )
    @Valid
    private Collection<FilterField> filterFields;

    @Join( relation = Relation.ONE_TO_ONE,
           cascades = Cascade.ALL,
           nullable = true,
           joinTables = @JoinTable( aliasName = "reportPathMapping",
                                    joinColumns = @JoinColumn( referenceColumn = ANAFILTERFIELD.VIEW_ID ),
                                    entityClass = ReportPathMapping.class ) )
    private ReportPathMapping reportPathMapping;
}
