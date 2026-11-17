package com.tlc.crm.core.api.analytics.integ.general.analytics.builders.layout.fields;

import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.filter.FilterFunctionMapper;
import com.tlc.analytics.config.model.view.report.ReportFieldCustomRange;
import com.tlc.analytics.config.model.view.report.ReportField;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Objects;

/**
 * Implementation of the {@link FieldBuilder} interface.
 *
 * @author james
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class FieldBuilderImpl
{

    private static class Instance
    {
        private static final FieldBuilderImpl INSTANCE = new FieldBuilderImpl();
    }

    public static FieldBuilderImpl INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * Creates a {@link FilterField} with the specified parameters.
     *
     * @param orgId          The ID of the organization associated with the filter field.
     * @param columnId       The ID of the column that this filter field pertains to.
     * @param cratedTime     The creation time of the filter field.
     * @param modifiedTime   The last modified time of the filter field.
     * @param order          The order in which the filter field should be applied.
     * @param operationId    The operation ID associated with this filter field.
     * @param filterType     The type of the filter being created.
     * @param operatorType   The type of operator for the filter (e.g., equals, greater than).
     * @param value          The value to be filtered.
     * @param from           The starting range value for the filter (optional).
     * @param to             The ending range value for the filter (optional).
     * @param functionName   Represents the function name for the filter ( optional)
     * @param isCustomFilter A flag indicates the custom filter
     * @param havingOperationType Represents the {@link OperationType} ID.
     * @return A {@link FilterField} object configured with the provided parameters.
     */
    @NonNull
    static FilterField getFilterField( final @NonNull Long orgId,
                                       final Long columnId,
                                       final Long actualColumnId,
                                       final @NonNull Long cratedTime,
                                       final @NonNull Long modifiedTime,
                                       final @NonNull Long order,
                                       final int operationId,
                                       final @NonNull Integer filterType,
                                       final @NonNull Integer operatorType,
                                       final Integer storeTypeId,
                                       final Object value,
                                       final Long from,
                                       final Long to,
                                       final String functionName,
                                       final boolean isCustomFilter,
                                       final Long fieldId,
                                       final Integer havingOperationType,
                                       final String configAsString,
                                       final String fieldName,
                                       final String customFieldConfig)
    {
        return FilterField.builder()
                          .orgId( orgId )
                          .columnId( columnId )
                          .actualColumnId( actualColumnId )
                          .createdTime( cratedTime )
                          .modifiedTime( modifiedTime )
                          .order( order )
                          .operationId( operationId )
                          .filterType( filterType )
                          .operatorType( operatorType )
                          .value( value )
                          .from( from )
                          .to( to )
                          .filterFunctionMapper( getFilterFunctionMapper( functionName ) )
                          .isCustomFilter( isCustomFilter )
                          .fieldId( fieldId )
                          .havingOperationType( havingOperationType )
                          .caseWhenAsString(configAsString)
                          .fieldName(fieldName)
                          .storeTypeId(storeTypeId)
                          .customFieldConfigAsString(customFieldConfig)
                          .build();
    }

    /**
     * Creates a {@link ReportField} with the specified parameters.
     *
     * @param orgId        The ID of the organization associated with the report field.
     * @param columnId     The ID of the column that this report field pertains to.
     * @param cratedTime   The creation time of the report field.
     * @param modTime      The last modified time of the report field.
     * @param order        The order in which the report field should appear.
     * @param operationId  The operation ID associated with this report field.
     * @param axisType     The axis type for the report field (e.g., X or Y axis).
     * @param fieldType    The type of the report field (e.g., metric, dimension).
     * @param label        The label to be displayed for this report field.
     * @return A {@link ReportField} object configured with the provided parameters.
     */
    @NonNull
    static ReportField getReportField(final @NonNull Long orgId,
                                      final @NonNull Long columnId,
                                      final @NonNull Long cratedTime,
                                      final @NonNull Long modTime,
                                      final @NonNull Long order,
                                      final int operationId,
                                      final @NonNull Integer axisType,
                                      final @NonNull Integer fieldType,
                                      final @NonNull String label,
                                      final Integer storeTypeId,
                                      final Long from,
                                      final Long to,
                                      final Long groupByColumnId,
                                      final Long modularField, final String functionName, final Long timePeriodId,
                                      final Integer moduleId, final Integer subModuleId,
                                      final String caseWhenConfigAsString,
                                      final String fieldName,
                                      final String customFieldConfigAsString,
                                      final boolean isPreCalculated)
    {
        return ReportField.builder()
                          .orgId( orgId )
                          .columnId( columnId )
                          .createdTime( cratedTime )
                          .modifiedTime( modTime )
                          .order( order )
                          .operationId( operationId )
                          .axisType( axisType )
                          .fieldType( fieldType )
                          .label( label )
                          .reportFieldCustomRange( getReportCustomRange( from, to ) )
                          .groupByColumnId( groupByColumnId )
                          .fieldId( modularField )
                          .functionName(functionName)
                          .moduleId( moduleId)
                          .subModuleId( subModuleId )
                          .timePeriodId(timePeriodId)
                          .caseWhenAsString(caseWhenConfigAsString)
                          .fieldName(fieldName)
                          .storeTypeId(storeTypeId)
                          .customFieldConfigAsString(customFieldConfigAsString)
                          .isPreCalculated(isPreCalculated)
                          .build();
    }

    /**
     * <p>
     * Builds a custom range values for the report fields
     * </p>
     *
     * @param from Represents the starting point of the report range
     * @param to   Represents the ending point of the report range
     * @return the {@link ReportFieldCustomRange}
     */
    private static ReportFieldCustomRange getReportCustomRange(final Long from, final Long to )
    {
        return Objects.isNull( from ) || Objects.isNull( to ) ? null
                                                              : ReportFieldCustomRange.builder()
                                                                                 .from( from )
                                                                                 .to( to )
                                                                                 .build();
    }

    /**
     * <p>
     * Builds a function mapper for the filter fields
     * </p>
     *
     * @param functionName Represents the function name for applying in the filter
     * @return the {@link FilterFunctionMapper}
     */
    private static FilterFunctionMapper getFilterFunctionMapper(final String functionName )
    {
        return Objects.isNull( functionName ) ? null : FilterFunctionMapper.builder()
                                                                           .functionName( functionName )
                                                                           .build();
    }
}
