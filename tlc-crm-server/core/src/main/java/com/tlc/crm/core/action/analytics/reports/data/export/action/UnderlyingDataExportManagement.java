package com.tlc.crm.core.action.analytics.reports.data.export.action;

import com.tlc.commons.json.JsonObject;

import com.tlc.web.WebAction;

import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.validator.analytics.Validator;
import com.tlc.crm.security.permission.annotation.authorizer.AnalyticsAuthorizer;

import lombok.NonNull;

import java.util.function.Consumer;

/**
 * Web action to manage the export of underlying analytics report data.
 *
 * <p>The endpoint is exposed at {@code /crm/analytics/underlying/export} and supports
 * authorization and validation logic specific to analytics export workflows.</p>
 *
 * @author Saravanan
 * @version 1.0
 */
@AnalyticsAuthorizer
@WebAction( path = "/crm/analytics/underlying/export" )
public class UnderlyingDataExportManagement extends AbstractAnalyticsExport
{
    private final Validator validator;

    public UnderlyingDataExportManagement()
    {
        this.validator = Validator.INSTANCE();
    }

    /**
     * Validates the underlying request data asynchronously.
     * Overrides the async version because the underlying export validator
     * relies on an asynchronous callback for report field fetching.
     *
     * @param orgId             Organization ID
     * @param userId            User ID
     * @param data              Request payload as a {@link JsonObject}
     * @param actionType        Type of export action
     * @param completionHandler called with {@code null} on success, or the exception on failure
     */
    @Override
    protected void validateRequest( final @NonNull Long orgId,
                                    final @NonNull Long userId,
                                    final @NonNull JsonObject data,
                                    final @NonNull ActionType actionType,
                                    final @NonNull Consumer<Exception> completionHandler )
    {
        validator.underlyingExportDataValidator().validateRequest( orgId, userId, data, completionHandler );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Not used directly — the async overload is invoked instead.</p>
     */
    @Override
    protected void validateRequest( final @NonNull Long orgId,
                                    final @NonNull Long userId,
                                    final @NonNull JsonObject data,
                                    final @NonNull ActionType actionType ) throws Exception
    {
    }
}