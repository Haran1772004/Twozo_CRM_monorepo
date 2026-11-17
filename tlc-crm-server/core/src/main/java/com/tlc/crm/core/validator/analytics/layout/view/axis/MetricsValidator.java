package com.tlc.crm.core.validator.analytics.layout.view.axis;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.commons.util.executor.CodeBlockExecutor;
import com.tlc.crm.core.api.analytics.entity.config.axis.AxisTypes;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModuleType;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularFieldConfig;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.service.util.ModularFieldUtil;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.RequestValidator;

import com.tlc.crm.core.validator.analytics.layout.filter.FilterValidator;
import com.tlc.crm.core.validator.analytics.layout.filter.PeriodicFilterValidator;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Provides an abstract way to validate the metrics configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see RequestValidator
 */
public class MetricsValidator
{
    private final FilterValidator filterValidator;
    private final PeriodicFilterValidator periodicFilterValidator;

    private MetricsValidator()
    {
        this.filterValidator = FilterValidator.INSTANCE();
        this.periodicFilterValidator = PeriodicFilterValidator.INSTANCE();
    }

    private static class Instance
    {
        private static final MetricsValidator INSTANCE = new MetricsValidator();
    }

    public static MetricsValidator INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                 Represents an organization id
     * @param jsonObject            Represents the {@link JsonObject} containing the configuration
     * @param reportModularFieldMap
     */
    public void validateRequest( final @NonNull Long orgId,
                                 final @NonNull JsonObject jsonObject,
                                 final Map<Long, ReportModularField> reportModularFieldMap) throws Exception
    {
        try
        {
            final ModuleType moduleType = ModuleType.getModuleType( jsonObject.getInt( CRMReportKeys.MODULE_ID ) );

            if( ModuleType.TIME_PERIOD.equals( moduleType ) )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.INVALID_MODULE,
                                     "i18n.analytics.config.invalid.module" );
            }
        }
        catch ( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_MODULE,
                                 "i18n.analytics.config.invalid.module" );
        }

        final Collection<ReportModularField> allReportFields = reportModularFieldMap.values();
        final Long fieldId = jsonObject.getLong(CRMReportKeys.FIELD_ID);

        final ReportModularField measureReportModularField = allReportFields.stream().filter(reportModularField -> Objects.equals(reportModularField.id(), fieldId))
                .findFirst()
                .orElseThrow(() -> ErrorCode.get(AnalyticsErrorCodes.MEASURE_FIELD_MISMATCHED,
                        "i18n.analytics.config.invalid.measure.field"));

        final ModularField measureModularField = measureReportModularField.getModularField();
        final Collection<Integer> measureIds = measureModularField.getModularFieldConfig().getMeasureIds();

        if (!measureIds.contains(jsonObject.getInt(CRMReportKeys.MEASURE_ID))) {
            throw ErrorCode.get(AnalyticsErrorCodes.MEASURE_FIELD_MISMATCHED,
                    "i18n.analytics.config.invalid.measure.field");
        }

        final Collection<ReportModularField> reportModularFields = ModularFieldUtil.filterByModule(jsonObject.getInt(CRMReportKeys.MODULE_ID), allReportFields);

        final Collection<Long> axisFields = new HashSet<>();
        final Map<AxisTypes, Collection<Long>> modularFieldMap = new HashMap<>();

        if(reportModularFields.isEmpty())
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_MODULE,
                                 "i18n.analytics.config.invalid.module" );
        }

        CodeBlockExecutor.forEachSafely(reportModularFields, reportField ->
                {
                    final ModularField modularField = reportField.getModularField();

                    if (modularField != null) {
                        final ModularFieldConfig config = modularField.getModularFieldConfig();

                        if (config != null && config.getAxisType() != null) {
                            final AxisTypes axisType = AxisTypes.getAxisTypes(config.getAxisType());

                            modularFieldMap.computeIfAbsent(axisType, k -> new ArrayList<>())
                                    .add(reportField.id());
                        }
                    }
                });

        if( modularFieldMap.containsKey( AxisTypes.X_Y ) )
        {
            axisFields.addAll( modularFieldMap.get( AxisTypes.X_Y ) );
        }

        if( modularFieldMap.containsKey( AxisTypes.Y_ONLY ) )
        {
            axisFields.addAll( modularFieldMap.get( AxisTypes.Y_ONLY ) );
        }

        if( !axisFields.contains( fieldId )  )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.MEASURE_FIELD_MISMATCHED,
                                 "i18n.analytics.config.invalid.measure.field" );
        }

        if( jsonObject.containsKey( CRMViewPropertyKeys.FILTERS ) )
        {
            final JsonArray jsonArray = jsonObject.getJsonArray( CRMViewPropertyKeys.FILTERS );
            final int totalMetrics = jsonArray.size();
            final Collection<Long> filterOrderIds = new HashSet<>();

            for( int index = 0; index < totalMetrics; index++ )
            {
                final JsonObject filterJson = jsonArray.getJsonObject( index );
                final Long order = filterJson.getLong( CRMViewPropertyKeys.ORDER );

                if( 1 > order || 10 < order )
                {
                    throw ErrorCode.get( AnalyticsErrorCodes.INVALID_FILTER_ORDER,
                                         "i18n.analytics.config.view.filter.invalid.order" );
                }

                if( filterOrderIds.contains( order ) )
                {
                    throw ErrorCode.get( AnalyticsErrorCodes.FILTER_ORDER_ALREADY_EXISTS,
                                         "i18n.analytics.config.view.filter.order.exists" );
                }

                filterOrderIds.add( order );
                filterValidator.validateRequest( orgId, jsonArray.getJsonObject( index ), reportModularFieldMap );
            }
        }

        if (!jsonObject.containsKey(CRMViewPropertyKeys.PERIODIC_FILTERS))
        {
            throw ErrorCode.get( AnalyticsErrorCodes.PERIODIC_FILTER_SHOULD_NOT_BE_EMPTY );
        }

        if (jsonObject.containsKey( CRMViewPropertyKeys.PERIODIC_FILTERS ))
        {
            periodicFilterValidator.validateRequest( orgId, jsonObject.getJsonObject( CRMViewPropertyKeys.PERIODIC_FILTERS  ), reportModularFieldMap );
        }
        final String label = jsonObject.getString( CRMViewPropertyKeys.LABEL );

        if( Objects.isNull( label ) || label.isBlank() )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.LABEL_SHOULD_NOT_BE_EMPTY,
                                 "i18n.analytics.config.view.invalid.label" );
        }

        if (NumericalConstant.TWO_HUNDRED_FIFTY_FIVE < label.length())
        {
            throw ErrorCode.get( AnalyticsErrorCodes.LABEL_NAME_LIMIT_EXCEEDED,
                                 "i18n.analytics.config.view.label.limit.exceeded" );
        }
    }
}
