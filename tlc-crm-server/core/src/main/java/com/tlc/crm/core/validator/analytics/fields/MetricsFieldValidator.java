package com.tlc.crm.core.validator.analytics.fields;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMViewType;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.RequestValidator;

import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to validate the metrics field configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see RequestValidator
 */
class MetricsFieldValidator implements RequestValidator<JsonObject>
{
    private MetricsFieldValidator()
    {
    }

    private static class Instance
    {
        private static final MetricsFieldValidator INSTANCE = new MetricsFieldValidator();
    }

    public static MetricsFieldValidator INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents the user id
     * @param jsonObject Represents the {@link JsonObject} containing the configuration
     */
    @Override
    public void validateRequest( final @NonNull Long orgId,
                                 final @NonNull Long userId,
                                 final @NonNull JsonObject jsonObject ) throws Exception
    {
        try
        {
            CRMViewType.getCrmViewType( jsonObject.getInt( CRMReportKeys.VIEW_TYPE_ID ) );
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_VIEW_TYPE,
                                 "i18n.analytics.config.invalid.view.type" );
        }
    }
}
