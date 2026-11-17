package com.tlc.crm.core.validator.analytics.layout.view;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.pk.export.Target;
import com.tlc.pk.export.ExportType;

import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.RequestValidator;

import lombok.NonNull;

import java.util.Arrays;
import java.util.Objects;

/**
 * <p>
 * Provides an abstract way to validate the view apply configuration
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see RequestValidator
 */
public class ViewDataExportValidator implements RequestValidator<JsonObject>
{
    private final CRMViewManagerAPI crmViewManager;

    private static class Instance
    {
        private static final ViewDataExportValidator INSTANCE = new ViewDataExportValidator();
    }

    private ViewDataExportValidator()
    {
        this.crmViewManager = CRMViewManagerAPI.INSTANCE();
    }

    public static ViewDataExportValidator INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param userId  Represents the user id
     * @param request Represents the {@link JsonObject} containing the configuration
     */
    @Override
    public void validateRequest( final @NonNull Long orgId,
                                 final @NonNull Long userId,
                                 final @NonNull JsonObject request) throws Exception
    {
        final CRMViewConfigManager<CRMChildViewDetails> crmChildViewManager = crmViewManager.getCRMChildViewManager();
        final CRMChildViewDetails crmChildViewDetails = crmChildViewManager.get(orgId, request.getLong( CRMReportKeys.VIEW_ID ) );

        Arrays.stream( Target.values() )
              .filter( target -> target.getTarget() == request.getInt( CRMReportKeys.TARGET ) )
              .findFirst().orElseThrow( ()-> ErrorCode.get( AnalyticsErrorCodes.INVALID_TARGET,
                                                            "i18n.analytics.config.invalid.target" ) );

        if ( Objects.nonNull( crmChildViewDetails ) )
        {
            if ( !Objects.equals( crmChildViewDetails.getViewTypeId(), request.getInt(CRMReportKeys.VIEW_TYPE_ID ) ) )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.INVALID_VIEW_TYPE,
                                     "i18n.analytics.config.invalid.view.type" );
            }

            if ( !Objects.equals( ExportType.CSV.getExportType(), request.getInt( CRMReportKeys.EXPORT_TYPE ) ) )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.INVALID_EXPORT_TYPE,
                                     "i18n.analytics.config.invalid.export.type" );
            }
        }
        else
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_VIEW_ID,
                                 "i18n.analytics.config.view.invalid.id" );
        }
    }
}
