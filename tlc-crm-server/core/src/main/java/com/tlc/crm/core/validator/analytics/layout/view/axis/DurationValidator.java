package com.tlc.crm.core.validator.analytics.layout.view.axis;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMKPIKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.RequestValidator;

import lombok.NonNull;

import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Provides an abstract way to validate the duration configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see RequestValidator
 */
public class DurationValidator
{

    /**
     * Creates duration validator dependencies.
     */
    private DurationValidator()
    {
    }

    private static class Instance
    {
        private static final DurationValidator INSTANCE = new DurationValidator();
    }

    /**
     * Returns singleton instance.
     *
     * @return {@link DurationValidator} singleton
     */
    public static DurationValidator INSTANCE()
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
        final Long fieldId    = jsonObject.getLong( CRMReportKeys.FIELD_ID );
        final int timeUnitId = jsonObject.getInt( CRMKPIKeys.TIME_UNIT_ID );
        final String label    = jsonObject.getString( CRMViewPropertyKeys.LABEL );
        final OperationType kpiTimeUnit;

        try
        {
            final ReportModularField reportModularField = reportModularFieldMap.get(fieldId);
            final ModularField modularField = (reportModularField != null) ? reportModularField.getModularField() : null;

            if( Objects.isNull( reportModularField ) || Objects.isNull( modularField ) || !modularField.getModularFieldConfig().isDurationAllowed() )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.MEASURE_FIELD_MISMATCHED,
                                     "i18n.analytics.config.invalid.measure.field" );
            }
        }
        catch ( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.MEASURE_FIELD_MISMATCHED,
                                 "i18n.analytics.config.invalid.measure.field" );
        }

        try
        {
            kpiTimeUnit = OperationType.getOperationType(timeUnitId);
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_TIME_UNIT,
                                 "i18n.analytics.config.invalid.time.unit" );
        }

        if( Objects.equals( CRMKPIKeys.CUSTOM, kpiTimeUnit.name().toLowerCase() ) && ( !jsonObject.containsKey( CRMKPIKeys.FROM ) ||
                                                                                  !jsonObject.containsKey( CRMKPIKeys.TO ) ) )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.CUSTOM_TYPE_REQUIRE_RANGE_VALUE,
                                 "i18n.analytics.config.invalid.custom.type" );
        }

        if( Objects.isNull( label ) || label.isBlank() )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.LABEL_SHOULD_NOT_BE_EMPTY,
                                 "i18n.analytics.config.view.invalid.label" );
        }

        if( 100 < label.length() )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.LABEL_NAME_LIMIT_EXCEEDED,
                                 "i18n.analytics.config.view.label.limit.exceeded" );
        }
    }
}
