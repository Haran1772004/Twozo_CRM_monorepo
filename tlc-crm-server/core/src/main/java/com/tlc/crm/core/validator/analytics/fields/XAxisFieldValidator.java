package com.tlc.crm.core.validator.analytics.fields;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.executor.CodeBlockExecutor;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModuleType;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.RequestValidator;

import lombok.NonNull;

import java.util.Collection;
import java.util.HashSet;

/**
 * <p>
 * Provides an abstract way to validate the field configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see RequestValidator
 */
class XAxisFieldValidator implements RequestValidator<JsonObject>
{
    private XAxisFieldValidator() {}

    private static class Instance
    {
        private static final XAxisFieldValidator INSTANCE = new XAxisFieldValidator();
    }

    /**
     * Returns singleton instance.
     *
     * @return {@link XAxisFieldValidator} singleton
     */
    public static XAxisFieldValidator INSTANCE()
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
        final JsonArray jsonArray = jsonObject.getJsonArray( CRMReportKeys.MODULE_IDS );
        final Collection<Integer> moduleIds = new HashSet<>();
        final int totalModules = jsonArray.size();

        if( 0 == totalModules )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.MODULE_SHOULD_NOT_BE_EMPTY,
                         "i18n.analytics.config.invalid.modules" );
        }

        if( 2 < totalModules )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.MULTIPLE_MODULE_NOT_SUPPORTED,
                                 "i18n.analytics.config.multiple.module.not.supported" );
        }

        for( int index = 0; index < totalModules;  index++ )
        {
            moduleIds.add( jsonArray.getInt( index ) );
        }

        CodeBlockExecutor.forEachSafely( moduleIds,
                                         module ->
                                         {
                                             if( ModuleType.TIME_PERIOD.equals( ModuleType.getModuleType( module ) ) )
                                             {
                                                 throw ErrorCode.get( AnalyticsErrorCodes.INVALID_MODULE,
                                                              "i18n.analytics.config.invalid.module" );
                                             }
                                         } );
    }
}
