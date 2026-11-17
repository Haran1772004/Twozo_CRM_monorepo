package com.tlc.crm.core.validator.analytics.layout.view;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.listeners.Callback;

import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMViewType;
import com.tlc.crm.core.api.analytics.service.module.ReportModularFieldService;
import com.tlc.crm.core.api.analytics.service.util.ModularFieldUtil;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.RequestValidator;

import com.tlc.pk.export.Target;
import com.tlc.pk.export.ExportType;

import lombok.NonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Validates export requests for viewing underlying data in analytics dashboards.
 * Ensures that the export type, category, view ID, view type, and module fields are valid.
 *
 * @author Saravanan
 * @version 1.0
 * @see RequestValidator
 */
public class ViewUnderlyingDataExportValidator implements RequestValidator<JsonObject>
{
    private final CRMViewManagerAPI crmViewManager;
    private final ReportModularFieldService reportModularFieldService;

    /**
     * Creates underlying-data export validator dependencies.
     */
    private ViewUnderlyingDataExportValidator()
    {
        this.crmViewManager = CRMViewManagerAPI.INSTANCE();
        this.reportModularFieldService = ReportModularFieldService.getInstance();
    }

    private static class Instance
    {
        private static final ViewUnderlyingDataExportValidator INSTANCE = new ViewUnderlyingDataExportValidator();
    }

    /**
     * Returns singleton instance.
     *
     * @return {@link ViewUnderlyingDataExportValidator} singleton
     */
    public static ViewUnderlyingDataExportValidator INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * Synchronous validation is not supported because this validator relies on an
     * asynchronous callback ({@code getByOrgId}). Use {@link #validateRequest(Long, Long, JsonObject, Consumer)} instead.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void validateRequest( final @NonNull Long orgId,
                                 final @NonNull Long userId,
                                 final @NonNull JsonObject jsonObject ) throws Exception
    {
    }

    /**
     * Validates the given JSON export request asynchronously.
     * The completionHandler is called with {@code null} on success,
     * or with the validation exception on failure.
     *
     * @param orgId             the organization ID
     * @param userId            the user ID making the request
     * @param jsonObject        the request payload to validate
     * @param completionHandler called with {@code null} on success, or the exception on failure
     */
    @Override
    public void validateRequest( final @NonNull Long orgId,
                                 final @NonNull Long userId,
                                 final @NonNull JsonObject jsonObject,
                                 final @NonNull Consumer<Exception> completionHandler )
    {
        try
        {
            final CRMViewConfigManager<CRMChildViewDetails> crmChildViewManager = crmViewManager.getCRMChildViewManager();

            if ( !Objects.equals( ExportType.CSV.getExportType(), jsonObject.getInt( CRMReportKeys.EXPORT_TYPE ) ) )
            {
                completionHandler.accept( ErrorCode.get( AnalyticsErrorCodes.INVALID_EXPORT_TYPE,
                                                         "i18n.analytics.config.invalid.export.type" ) );
                return;
            }

            Arrays.stream( Target.values() )
                  .filter( target -> target.getTarget() == jsonObject.getInt( CRMReportKeys.TARGET ) )
                  .findFirst().orElseThrow( () -> ErrorCode.get( AnalyticsErrorCodes.INVALID_TARGET,
                                                                 "i18n.analytics.config.invalid.target" ) );

            if ( !crmChildViewManager.exists( orgId, jsonObject.getLong( CRMReportKeys.VIEW_ID ) ) )
            {
                completionHandler.accept( ErrorCode.get( AnalyticsErrorCodes.INVALID_VIEW_ID,
                                                         "i18n.analytics.config.view.invalid.id" ) );
                return;
            }

            if ( !CRMViewType.isValidTypeId( jsonObject.getInt( CRMReportKeys.VIEW_TYPE_ID ) ) )
            {
                completionHandler.accept( ErrorCode.get( AnalyticsErrorCodes.INVALID_VIEW_TYPE,
                                                         "i18n.analytics.config.invalid.view.type" ) );
                return;
            }

            reportModularFieldService.getByOrgId( orgId, new Callback<>()
            {
                @Override
                public void onSuccess( final Map<Long, ReportModularField> reportFieldMap )
                {
                    try
                    {
                        final Collection<ReportModularField> filteredFields =
                                ModularFieldUtil.filterByModule( jsonObject.getInt( CRMReportKeys.MODULE_ID ),
                                                                reportFieldMap.values() );

                        if ( filteredFields.isEmpty() )
                        {
                            completionHandler.accept( ErrorCode.get( AnalyticsErrorCodes.INVALID_MODULE,
                                                                     "i18n.analytics.config.invalid.module" ) );
                            return;
                        }

                        if ( jsonObject.containsKey( CRMViewPropertyKeys.SELECT ) )
                        {
                            final Collection<Long> moduleFieldIds = filteredFields.stream()
                                                                                 .map( ReportModularField::id )
                                                                                 .collect( Collectors.toSet() );

                            final JsonArray jsonArray = jsonObject.getJsonArray( CRMViewPropertyKeys.SELECT );
                            final int totalFields = jsonArray.size();

                            for ( int index = 0; index < totalFields; index++ )
                            {
                                if ( !moduleFieldIds.contains( jsonArray.getLong( index ) ) )
                                {
                                    completionHandler.accept( ErrorCode.get( AnalyticsErrorCodes.INVALID_CONFIGURED_FIELDS,
                                                                             "i18n.analytics.config.invalid.configured.fields" ) );
                                    return;
                                }
                            }
                        }

                        completionHandler.accept( null );
                    }
                    catch ( final Exception exception )
                    {
                        completionHandler.accept( exception );
                    }
                }

                @Override
                public void onFailure( final Throwable cause )
                {
                    completionHandler.accept( ErrorCode.get( AnalyticsErrorCodes.INVALID_MODULE,
                                                             "i18n.analytics.config.invalid.module" ) );
                }
            } );
        }
        catch ( final Exception exception )
        {
            completionHandler.accept( exception );
        }
    }
}
