package com.tlc.crm.core.validator.analytics.layout.filter;

import com.tlc.analytics.config.model.view.filter.OperatorType;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.entity.config.fields.AnalyticsField;
import com.tlc.crm.core.api.analytics.entity.config.function.CRMFunction;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSystemModularField;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.manager.CRMReportManagerAPI;
import com.tlc.crm.core.api.analytics.service.field.AnalyticsFieldService;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.models.StoreType;

import com.tlc.crm.fields.service.FieldTypeService;

import lombok.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides an abstract way to validate the periodic filter configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public class PeriodicFilterValidator
{
    private final CRMReportManagerAPI crmManager;
    private final FieldTypeService fieldTypeService;
    private final AnalyticsFieldService analyticsFieldService;

    private PeriodicFilterValidator()
    {
        this.crmManager = CRMReportManagerAPI.INSTANCE();
        this.fieldTypeService = FieldTypeService.getInstance();
        this.analyticsFieldService = AnalyticsFieldService.getInstance();
    }

    private static class Instance
    {
        private static final PeriodicFilterValidator INSTANCE = new PeriodicFilterValidator();
    }

    public static PeriodicFilterValidator INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param jsonObject Represents the {@link JsonObject} containing the configuration
     */
    public void validateRequest( final @NonNull Long orgId,
                                 final @NonNull JsonObject jsonObject,
                                 final Map<Long, ReportModularField> reportModularFieldMap) throws Exception
    {
        final Long fieldId = jsonObject.getLong( CRMReportKeys.FIELD_ID );
        final int comparatorId = jsonObject.getInt( CRMReportKeys.COMPARATOR_ID );
        final ReportModularField reportModularField = reportModularFieldMap.get(fieldId);

        if( Objects.isNull( reportModularField ) )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.MEASURE_FIELD_MISMATCHED,
                                 "i18n.analytics.config.invalid.measure.field" );
        }

        final ModularField modularField = reportModularField.getModularField();

        if( !modularField.getModularFieldConfig().isPeriodicFilterAllowed() )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.MEASURE_FIELD_MISMATCHED,
                                 "i18n.analytics.config.invalid.measure.field" );
        }
        final Long crmFieldId = modularField.getCrmFieldId();
        Long anaFieldId = null;

        if (!reportModularField.isCustom()) {
            anaFieldId = modularField instanceof CRMSystemModularField crmSystemModularField ? crmSystemModularField.getAnaFieldId() : null;
        }

        if( Objects.isNull( crmFieldId ) )
        {
            final AnalyticsField anaField = analyticsFieldService.getAnalyticsFieldById( anaFieldId );

            if( Objects.isNull( anaField ) )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.MEASURE_FIELD_MISMATCHED,
                                     "i18n.analytics.config.invalid.measure.field" );
            }
        }

        if( Objects.isNull( anaFieldId ) )
        {
            final FieldInfo fieldInfo = FieldService.getInstance()
                                                    .getField( orgId, crmFieldId );

            if( Objects.isNull( fieldInfo ) )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.MEASURE_FIELD_MISMATCHED,
                                     "i18n.analytics.config.invalid.measure.field" );
            }
            final StoreType storeType = fieldInfo.storeType();

            if( Objects.isNull( storeType ) )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.MEASURE_FIELD_MISMATCHED,
                                     "i18n.analytics.config.invalid.measure.field" );
            }
            final Collection<OperatorType> supportedOperators = OperatorType.getSupportedOperators(storeType);

            if( Objects.isNull( supportedOperators ) || supportedOperators.isEmpty() )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.MEASURE_FIELD_MISMATCHED,
                        "i18n.analytics.config.invalid.measure.field" );
            }
            final Collection<Integer> comparatorIds = supportedOperators.stream().map(OperatorType::getTypeCode)
                    .collect(Collectors.toSet());

            if( !comparatorIds.contains( comparatorId ) )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.INVALID_COMPARATOR,
                        "i18n.analytics.config.invalid.operator" );
            }
        }
        final OperatorType comparator = OperatorType.getOperatorType(comparatorId);

        if( Objects.isNull( comparator ) || !comparator.isPeriodicFilterEnabled() )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_COMPARATOR,
                                 "i18n.analytics.config.invalid.operator" );
        }

        if( jsonObject.containsKey( CRMViewPropertyKeys.FILTER_VALUES ) && comparator.isHasValue() )
        {
            final JsonArray jsonArray = jsonObject.getJsonArray( CRMViewPropertyKeys.VALUES );
            final int totalSize = jsonArray.size();

            if( 0 == totalSize )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.FILTER_VALUES_SHOULD_NOT_BE_EMPTY,
                                     "i18n.analytics.config.filter.invalid.values" );
            }

            if( 1 < totalSize )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.FILTER_VALUES_LIMIT_EXCEEDED,
                                     "i18n.analytics.config.filter.values.limit.exceeded" );
            }
            final Object value = jsonArray.getJsonObject( 0 )
                                          .get( CRMViewPropertyKeys.VALUE );

            if( Objects.isNull( value ) )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.FILTER_VALUE_SHOULD_NOT_BE_EMPTY,
                                     "i18n.analytics.config.filter.invalid.value" );
            }

            if( value instanceof final String string && string.isBlank() )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.FILTER_VALUE_SHOULD_NOT_BE_EMPTY,
                                     "i18n.analytics.config.filter.invalid.value" );
            }

            if( value instanceof final Map<?, ?> valueMap && valueMap.isEmpty() )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.FILTER_VALUE_SHOULD_NOT_BE_EMPTY,
                                     "i18n.analytics.config.filter.invalid.value" );
            }
        }

        if( jsonObject.containsKey( CRMReportKeys.FUNCTION_ID ) )
        {
            final boolean functionIdExists = CRMFunction.getSupportedFunctions(comparatorId).stream().filter(Objects::nonNull)
                    .map(CRMFunction::getTypeCode).anyMatch(functionId -> functionId.equals(jsonObject.getInt( CRMReportKeys.FUNCTION_ID )));

            if( !functionIdExists )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.INVALID_FUNCTION,
                                     "i18n.analytics.config.invalid.function" );
            }
        }
    }
}
