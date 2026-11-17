package com.tlc.crm.core.validator.analytics.layout.view.axis;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.entity.config.widget.WidgetCompareUnit;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMKPIKeys;
import com.tlc.crm.core.api.analytics.manager.CRMReportManagerAPI;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.RequestValidator;

import lombok.NonNull;

import java.util.Objects;

/**
 * <p>
 * Provides an abstract way to validate the compare to configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see RequestValidator
 */
class CompareToValidator implements RequestValidator<JsonObject>
{
    private final CRMReportManagerAPI crmManager;

    private static class Instance
    {
        private static final CompareToValidator INSTANCE = new CompareToValidator();
    }

    /**
     * Creates compare-to validator dependencies.
     */
    private CompareToValidator()
    {
        this.crmManager = CRMReportManagerAPI.INSTANCE();
    }

    /**
     * Returns singleton instance.
     *
     * @return {@link CompareToValidator} singleton
     */
    public static CompareToValidator INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents the user id
     * @param request Represents the {@link JsonObject} containing the configuration
     */
    @Override
    public void validateRequest( final @NonNull Long orgId,
                                 final @NonNull Long userId,
                                 final @NonNull JsonObject request ) throws Exception
    {
        try
        {
             final WidgetCompareUnit widgetCompareUnit = crmManager.getWidgetCompareManager()
                                                                   .get( orgId, request.getLong( CRMKPIKeys.COMPARE_UNIT_ID ) );

            if( Objects.isNull( widgetCompareUnit ) )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.INVALID_COMPARE_UNIT,
                                     "i18n.analytics.config.widget.invalid.compare.unit" );
            }
        }
        catch ( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_COMPARE_UNIT,
                                 "i18n.analytics.config.widget.invalid.compare.unit" );
        }
    }
}
