package com.tlc.crm.core.validator.analytics.layout.view;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.listeners.Callback;

import com.tlc.commons.util.constants.NumericalConstant;
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

import lombok.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Validates the view underlying configuration.
 *
 * <p>The sync {@link #validateRequest(Long, Long, JsonObject)} is a no-op.
 * The async {@link #validateRequest(Long, Long, JsonObject, Consumer)} fetches
 * {@code reportFieldMap} via its own callback, validates inside that callback,
 * and signals completion — fully async, no shared state.</p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
class ViewUnderlyingValidator implements RequestValidator<JsonObject>
{
    private final CRMViewManagerAPI crmViewManager;
    private final ReportModularFieldService reportModularFieldService;

    private ViewUnderlyingValidator()
    {
        this.crmViewManager = CRMViewManagerAPI.INSTANCE();
        this.reportModularFieldService = ReportModularFieldService.getInstance();
    }

    private static class Instance
    {
        private static final ViewUnderlyingValidator INSTANCE = new ViewUnderlyingValidator();
    }

    public static ViewUnderlyingValidator INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * No-op — async override handles everything.
     */
    @Override
    public void validateRequest( final @NonNull Long orgId,
                                 final @NonNull Long userId,
                                 final @NonNull JsonObject jsonObject ) throws Exception
    {
    }

    /**
     * Async validation: validates view ID &amp; type synchronously, then fetches
     * {@code reportFieldMap} via callback and validates report fields inside
     * that callback.
     */
    @Override
    public void validateRequest( final @NonNull Long orgId,
                                 final @NonNull Long userId,
                                 final @NonNull JsonObject jsonObject,
                                 final @NonNull Consumer<Exception> completionHandler )
    {
        final CRMViewConfigManager<CRMChildViewDetails> crmChildViewManager = crmViewManager.getCRMChildViewManager();

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

        reportModularFieldService.getByOrgId(orgId, new Callback<>() {
            @Override
            public void onSuccess( final Map<Long, ReportModularField> reportFieldMap )
            {
                try
                {
                    final Collection<ReportModularField> reportModularFields =
                            ModularFieldUtil.filterByModule( jsonObject.getInt( CRMReportKeys.MODULE_ID ),
                                                             reportFieldMap.values() );

                    if ( reportModularFields.isEmpty() )
                    {
                        completionHandler.accept( ErrorCode.get( AnalyticsErrorCodes.INVALID_MODULE,
                                                                 "i18n.analytics.config.invalid.module" ) );
                        return;
                    }

                    if ( jsonObject.containsKey( CRMViewPropertyKeys.SELECT ) )
                    {
                        final Collection<Long> reportModularFieldIds = reportModularFields.stream().map( ReportModularField::id )
                                .collect( Collectors.toSet() );
                        final JsonArray jsonArray = jsonObject.getJsonArray( CRMViewPropertyKeys.SELECT );
                        final int totalFields = jsonArray.size();

                        for (int index = NumericalConstant.ZERO; index < totalFields; index++ )
                        {
                            if ( !reportModularFieldIds.contains( jsonArray.getLong( index ) ) )
                            {
                                completionHandler.accept(ErrorCode.get( AnalyticsErrorCodes.INVALID_CONFIGURED_FIELDS));
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
                completionHandler.accept(ErrorCode.get( AnalyticsErrorCodes.INVALID_CONFIGURED_FIELDS));
            }
        });
    }
}
