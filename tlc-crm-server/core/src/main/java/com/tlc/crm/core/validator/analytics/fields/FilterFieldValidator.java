package com.tlc.crm.core.validator.analytics.fields;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.entity.config.modules.ModuleType;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.RequestValidator;

import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to validate the filter field configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see RequestValidator
 */
class FilterFieldValidator implements RequestValidator<JsonObject>
{
    private FilterFieldValidator() {}

    private static class Instance
    {
        private static final FilterFieldValidator INSTANCE = new FilterFieldValidator();
    }

    /**
     * Returns singleton instance.
     *
     * @return {@link FilterFieldValidator} singleton
     */
    public static FilterFieldValidator INSTANCE()
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
            final ModuleType moduleType = ModuleType.getModuleType( jsonObject.getInt( CRMReportKeys.MODULE_ID ) );

            if( ModuleType.TIME_PERIOD.equals( moduleType ) )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.INVALID_MODULE,
                             "i18n.analytics.config.invalid.module" );
            }
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_MODULE,
                         "i18n.analytics.config.invalid.module" );
        }
    }
}
