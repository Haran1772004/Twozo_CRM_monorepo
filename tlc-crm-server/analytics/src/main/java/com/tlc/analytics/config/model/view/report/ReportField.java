package com.tlc.analytics.config.model.view.report;

import com.tlc.analytics.sql.resource.ANACUSTOMRANGE;
import com.tlc.analytics.sql.resource.ANAREPORTFIELD;
import com.tlc.analytics.config.keys.view.report.ReportFieldKeys;
import com.tlc.analytics.config.model.view.BaseField;
import com.tlc.analytics.config.model.view.report.chart.Axis;

import com.tlc.entity.annotation.Cascade;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Relation;
import com.tlc.entity.annotation.Table;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Collection;
import java.util.Objects;


/**
 * <p>
 * A model that manages the report field details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 * @see BaseField
 */
@SuperBuilder
@Getter
@Table( name = ANAREPORTFIELD.TABLE )
public class ReportField extends BaseField
{

    @JsonKey( key = ReportFieldKeys.AXIS_TYPE )
    @Column( name = ANAREPORTFIELD.AXIS )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.report.field.invalid.axis.type" )
    private Integer axisType;

    @JsonKey( key = ReportFieldKeys.FIELD_TYPE )
    @Getter( AccessLevel.NONE )
    @Column( name = ANAREPORTFIELD.FIELD_TYPE )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.report.field.invalid.field.type" )
    private Integer fieldType;

    @JsonKey( key = ReportFieldKeys.LABEL )
    @Column( name = ANAREPORTFIELD.LABEL )
    @NotEmpty( groups = { Group.Create.class, Group.Update.class },
               message = "i18n.analytics.report.field.invalid.label" )
    @Size( max = 15 )
    private String label;

    @JsonKey( key = ReportFieldKeys.RANGE_SIZE )
    @Column( name = ANAREPORTFIELD.RANGE_SIZE )
    @NotEmpty( groups = { Group.Create.class, Group.Update.class },
               message = "i18n.analytics.report.field.invalid.range" )
    private Long rangeSize;

    @Join( relation = Relation.ONE_TO_ONE,
           cascades = Cascade.ALL,
           nullable = true,
           joinTables = @JoinTable( joinColumns = @JoinColumn( referenceColumn = ANACUSTOMRANGE.REPORT_COLUMN_ID ),
                                                               entityClass = ReportFieldCustomRange.class ) )
    private ReportFieldCustomRange reportFieldCustomRange;

    @Column( name = ANAREPORTFIELD.GROUP_BY_COLUMN_ID )
    private Long groupByColumnId;

    @Column( name = ANAREPORTFIELD.FUNCTION_NAME )
    @Getter(AccessLevel.NONE)
    private String functionName;

    @Column( name = ANAREPORTFIELD.TIME_PERIOD_ID )
    private Long timePeriodId;

    @Column( name = ANAREPORTFIELD.MODULE_ID )
    private Integer moduleId;

    @Column( name = ANAREPORTFIELD.SUB_MODULE_ID )
    private Integer subModuleId;

    @Column(name = ANAREPORTFIELD.FIELD_NAME)
    private String fieldName;

    private Collection<ReportFieldSelectorColumn> selectorColumns;

    public Axis getAxis()
    {
        return Axis.getAxis( axisType );
    }

    public FieldCategory getFieldType()
    {
        return FieldCategory.get( fieldType );
    }

    public String getFunctionName()
    {
        return Objects.isNull(functionName) ? null : functionName.replaceAll("\\s+", "");
    }

}
