package com.tlc.analytics.config.model.view.filter;

import com.tlc.analytics.config.keys.table.TableKeys;
import com.tlc.analytics.config.keys.view.filter.FilterFieldKeys;
import com.tlc.analytics.sql.resource.ANAFILTERFIELD;
import com.tlc.analytics.sql.resource.ANAFILTERFUNCTIONMAPPER;

import com.tlc.entity.annotation.Cascade;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Relation;
import com.tlc.entity.annotation.Table;
import com.tlc.analytics.config.model.view.BaseField;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Collection;


/**
 * <p>
 * A model that manages the filter field details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANAFILTERFIELD.TABLE )
public class FilterField extends BaseField
{
    @JsonKey( key = FilterFieldKeys.FILTER_TYPE )
    @Column( name = ANAFILTERFIELD.FILTER_TYPE )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.filter.field.invalid.filter.type" )
    @Getter( AccessLevel.NONE )
    private Integer filterType;

    @JsonKey( key = FilterFieldKeys.OPERATOR_TYPE )
    @Column( name = ANAFILTERFIELD.OPERATOR )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.filter.field.invalid.operator.type" )
    @Getter( AccessLevel.NONE )
    private Integer operatorType;

    @JsonKey( key = FilterFieldKeys.VALUE )
    @Column( name = ANAFILTERFIELD.VALUE )
    private Object value;

    @JsonKey( key = FilterFieldKeys.VIEW_COLUMN_ID )
    @Column( name = ANAFILTERFIELD.VALUE_COLUMN_ID )
    private Long viewColumnId;

    @JsonKey( key = FilterFieldKeys.FROM )
    @Column( name = ANAFILTERFIELD.FROM )
    @Size( max = 100 )
    private Long from;

    @JsonKey( key = FilterFieldKeys.TO )
    @Column( name = ANAFILTERFIELD.TO )
    @Size( max = 100 )
    private Long to;

    @JsonKey( key = FilterFieldKeys.FORMAT )
    @Column( name = ANAFILTERFIELD.FORMAT )
    @Size( max = 100 )
    private String format;

    @JsonKey( key = TableKeys.CREATED_AT )
    @Getter( AccessLevel.NONE )
    @Column( name = ANAFILTERFIELD.CREATED_TIME )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.filter.field.invalid.created.time" )
    private Long createdTime;

    @JsonKey( key = FilterFieldKeys.MODIFIED_AT )
    @Getter( AccessLevel.NONE )
    @Column( name = ANAFILTERFIELD.MODIFIED_TIME )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.filter.field.invalid.modified.time" )
    private Long modifiedTime;

    @JsonKey( key = FilterFieldKeys.ANA_ACTUAL_COLUMN_ID )
    @Column( name = ANAFILTERFIELD.ANA_ACTUAL_COLUMN_ID )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.filter.field.invalid.actual.column" )
    private Long actualColumnId;

    @JsonKey( key = FilterFieldKeys.IS_CUSTOM_FILTER )
    @Column( name = ANAFILTERFIELD.IS_CUSTOM_FILTER )
    private boolean isCustomFilter;

    @JsonKey( key = FilterFieldKeys.HAVING_OPERATION_TYPE )
    @Column( name= ANAFILTERFIELD.HAVING_OPERATION_TYPE )
    private Integer havingOperationType;

    @Join( relation = Relation.ONE_TO_ONE,
            cascades = Cascade.ALL,
            nullable = true,
            joinTables = @JoinTable( joinColumns = @JoinColumn( referenceColumn = ANAFILTERFUNCTIONMAPPER.FILTER_FIELD_ID ),
                                     entityClass = FilterFunctionMapper.class ) )
    private FilterFunctionMapper filterFunctionMapper;

    @Column(name = ANAFILTERFIELD.FIELD_NAME)
    private String fieldName;

    private Collection<FilterFieldSelectorColumn> selectorColumns;

    public Filter getFilterType()
    {
        return Filter.getFilterType( filterType );
    }

    public OperatorType getOperatorType()
    {
        return OperatorType.getOperatorType( operatorType );
    }
}
