package com.tlc.crm.core.api.analytics.integ.general.analytics.builders.layout.fields;

import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.report.CaseWhenConfig;
import com.tlc.analytics.config.model.view.filter.FilterFieldSelectorColumn;
import com.tlc.analytics.config.model.view.report.ReportField;

import com.tlc.analytics.config.model.view.report.ReportFieldSelectorColumn;
import lombok.NonNull;

import java.util.Collection;
import java.util.Map;

/**
 * Contract for building fields within the CRM reporting framework.
 *
 * @author james
 * @version 1.0
 */
public interface FieldBuilder
{
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
    default FilterField getFilterField( final @NonNull Long orgId,
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
        return FieldBuilderImpl.getFilterField( orgId, columnId, actualColumnId, cratedTime,
                                                modifiedTime, order, operationId,
                                                filterType, operatorType, storeTypeId, value, from, to, functionName, isCustomFilter, fieldId , havingOperationType, configAsString, fieldName, customFieldConfig);
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
    default ReportField getReportField(final @NonNull Long orgId,
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
                                       final Long modularField,
                                       final String functionName, final Long timePeriodId, final Integer moduleId, final Integer subModuleId,
                                       final String caseWhenConfigAsString,
                                       final String fieldName,
                                       final String customFieldConfigAsString,
                                       final boolean isPreCalculated)
    {
        return FieldBuilderImpl.getReportField( orgId, columnId, cratedTime,
                                                modTime, order, operationId,
                                                axisType, fieldType, label, storeTypeId, from, to, groupByColumnId, modularField, functionName,
                timePeriodId, moduleId, subModuleId, caseWhenConfigAsString, fieldName, customFieldConfigAsString, isPreCalculated);
    }
}
