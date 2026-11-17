package com.tlc.crm.core.action.analytics.reports.data.export.action;

import com.tlc.analytics.config.action.AnalyticsRequest;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.action.analytics.ReportsAction;
import com.tlc.crm.core.action.analytics.reports.data.export.internal.AnalyticsExportHandler;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.security.iam.LoggedInUserInfo;

import com.tlc.email.common.model.address.EmailAddress;

import com.tlc.i18n.I18nResolver;
import com.tlc.pk.export.Target;

import lombok.NonNull;


/**
 * Abstract class for handling analytics data export functionality.
 *
 * <p>This class provides reusable methods to initiate and check the status of
 * data export operations (e.g., download or email) for analytics reports.</p>
 *
 * <p>It supports two export modes:
 * <ul>
 *     <li>{@code DOWNLOAD} - Direct download of exported data</li>
 *     <li>{@code EMAIL} - Email the exported data to the logged-in user</li>
 * </ul></p>
 *
 * <p>The actual action implementation classes (like {@link UnderlyingDataExportManagement}) and ({@link GraphDataExportManagement})
 * extend this to utilize the common export logic.</p>
 *
 * @author Saravanan
 * @version 1.0
 */
public abstract class AbstractAnalyticsExport extends ReportsAction
{

    /**
     * {@inheritDoc}
     *
     * <p>Handles only {@link ActionType#INIT_EXPORT} — delegates to {@link #initExport} to
     * build the export initialisation response and sends it. Throws for unsupported action types.</p>
     *
     * @param analyticsRequest the analytics request context, including org, user, and response handle
     * @param actionType       the type of action being performed, must not be null
     * @param requestData      the JSON payload containing request-specific data, must not be null
     * @param i18nResolver     an instance of {@link I18nResolver} for handling internationalization
     */
    @Override
    protected void processRequest(final @NonNull AnalyticsRequest analyticsRequest,
                                  final @NonNull ActionType actionType,
                                  final @NonNull JsonObject requestData,
                                  final @NonNull I18nResolver i18nResolver) {
        if ( actionType != ActionType.INIT_EXPORT )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_ACTION_TYPE,
                                 "i18n.analytics.invalid.action.type" );
        }

        final CrmResponse crmResponse = analyticsRequest.crmResponse();
        initExport(analyticsRequest, actionType, requestData, i18nResolver).forEach(crmResponse::put);
        crmResponse.send();
    }

    /**
     * Initiates the analytics data export process.
     *
     * <p>If the export mode is EMAIL, additional metadata like recipient email and username
     * are added to the request payload based on the logged-in user.</p>
     *
     * @param analyticsRequest The analytics request context
     * @param actionType       The action type being performed
     * @param requestData      Request data for the export
     * @param i18nResolver     Internationalization resolver for localized messages
     * @return A {@link JsonObject} containing the export ID if successful
     */

    protected @NonNull JsonObject initExport( final @NonNull AnalyticsRequest analyticsRequest,
                                              final @NonNull ActionType actionType,
                                              final @NonNull JsonObject requestData,
                                              final @NonNull I18nResolver i18nResolver )
    {
        final AnalyticsExportHandler analyticsExportHandler = AnalyticsExportHandler.getInstance();
        final JsonObject responseJson = Json.object();
        final int targetId = requestData.getInt( CRMReportKeys.TARGET );
        final Target Target = com.tlc.pk.export.Target.EMAIL;

        if ( targetId == Target.getTarget() )
        {
            final CrmUser crmUser = LoggedInUserInfo.getUser();
            final EmailAddress recipientEmail = new EmailAddress( crmUser.getEmail() );

            requestData.put( CRMReportKeys.RECIPIENT_EMAIL, recipientEmail.getEmailAddress() );
            requestData.put( CRMReportKeys.USER_NAME, crmUser.getFullName() );
        }

        try
        {
            final Long exportId = analyticsExportHandler.initiateExportProcess( analyticsRequest,
                                                                                actionType,
                                                                                requestData,
                                                                                i18nResolver );
            responseJson.put( CRMReportKeys.ID, exportId );
            return responseJson;
        }
        catch ( Exception exception )
        {
            throw ( targetId == Target.getTarget() )
                    ? ErrorCode.get( AnalyticsErrorCodes.EXPORT_VIA_EMAIL_ACTION_FAILED,
                      "i18n.analytics.config.export.via.email.action.failed" )
                    : ErrorCode.get( AnalyticsErrorCodes.DOWNLOAD_ACTION_FAILED,
                      "i18n.analytics.config.download.action.failed" );
        }
    }
}
