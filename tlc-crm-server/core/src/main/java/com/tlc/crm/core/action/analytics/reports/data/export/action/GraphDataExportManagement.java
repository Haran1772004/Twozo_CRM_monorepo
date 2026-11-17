package com.tlc.crm.core.action.analytics.reports.data.export.action;

import com.tlc.commons.json.JsonObject;

import com.tlc.web.WebAction;

import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.validator.analytics.Validator;
import com.tlc.crm.security.permission.annotation.authorizer.AnalyticsAuthorizer;

import lombok.NonNull;

/**
 * Web action to manage the export of Graph analytics report data.
 *
 * <p>The endpoint is exposed at {@code /crm/analytics/graph/export} and supports
 * authorization and validation logic specific to analytics export workflows.</p>
 *
 * @author Saravanan
 * @version 1.0
 */
@AnalyticsAuthorizer
@WebAction( path = "/crm/analytics/graph/export" )
public class GraphDataExportManagement extends AbstractAnalyticsExport
{
    private final Validator validator;

    public GraphDataExportManagement()
    {
        this.validator = Validator.INSTANCE();
    }

    /**
     * Validates the request data.
     *
     * @param orgId      Organization ID
     * @param userId     User ID
     * @param data       Request payload as a {@link JsonObject}
     * @param actionType Type of export action
     */
    @Override
    protected void validateRequest( final @NonNull Long orgId,
                                    final @NonNull Long userId,
                                    final @NonNull JsonObject data,
                                    final @NonNull ActionType actionType ) throws Exception
    {
        validator.exportDataValidator()
                 .validateRequest( orgId, userId, data );
    }
}