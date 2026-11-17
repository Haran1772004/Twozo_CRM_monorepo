package com.tlc.crm.core.action.analytics.reports.layout.dashboard;

import com.tlc.analytics.config.action.AnalyticsRequest;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.executor.CodeBlockExecutor;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;

import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.action.analytics.ReportsAction;
import com.tlc.crm.core.api.analytics.converter.CRMReportsAPI;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.common.CommonKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMDashboardKeys;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.Validator;

import com.tlc.crm.security.iam.LoggedInUserInfo;

import com.tlc.i18n.I18nResolver;

import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;

import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import lombok.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles the HTTP requests that are relevant to dashboard share related actions
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@WebAction( path = "/crm/analytics/dashboard/share" )
public class DashboardShareManagement extends ReportsAction
{
    private final Validator validator;
    private final CRMReportsAPI crmReports;
    private final UserService userService;
    private final OrganizationService organizationService;
    private final com.tlc.crm.common.iam.service.UserService iamUserService;

    public DashboardShareManagement()
    {
        this.validator = Validator.INSTANCE();
        this.crmReports = CRMReportsAPI.getInstance();
        this.userService = UserService.getInstance();
        this.organizationService = OrganizationService.getInstance();
        this.iamUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organization ID
     * @param userId     Represents the user ID
     * @param data       Which contains the requested data
     * @param actionType The type of action being performed
     * @throws Exception if any validation fails.
     */
    @Override
    protected void validateRequest( final @NonNull Long orgId,
                                    final @NonNull Long userId,
                                    final @NonNull JsonObject data,
                                    final @NonNull ActionType actionType ) throws Exception
    {
        switch( actionType )
        {
            case GET ->
            {
                validator.visibilityGetValidator()
                         .validateRequest( orgId, userId, data );
            }
            case UPDATE ->
            {
                validator.visibilityUpdateValidator()
                         .validateRequest( orgId, userId, data );
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to the dashboard share API based on the action type (SHARE, UNSHARE, LIST)
     * and sends the response. Throws for unsupported action types.</p>
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
        final CrmResponse crmResponse = analyticsRequest.crmResponse();

        switch( actionType )
        {
            case UPDATE ->
            {
                crmReports.getDashboardVisibility()
                        .share(analyticsRequest, requestData, i18nResolver)
                        .forEach(crmResponse::put);
                crmResponse.send();
            }
            case GET ->
            {
                final JsonObject responseJson = crmReports.getDashboardVisibility()
                                                          .getShareVisibility( analyticsRequest, requestData, i18nResolver );

                if( responseJson.containsKey( CRMDashboardKeys.USERS ) )
                {
                    final Long orgId = analyticsRequest.orgId();
                    final JsonArray jsonArray = responseJson.getJsonArray( CRMDashboardKeys.USERS );
                    final Map<Long, JsonObject> userJsons = new HashMap<>();
                    final int totalUsers = jsonArray.size();

                    for( int index = 0; index < totalUsers; index ++ )
                    {
                        final JsonObject jsonObject = jsonArray.getJsonObject( index );

                        userJsons.put( jsonObject.getLong( CRMReportKeys.ID ), jsonObject );
                    }
                    final Collection<Long> userIds = userJsons.keySet();
                    final Map<Long, Long> userIdsMap = iamUserService.getUsers( orgId, userIds )
                                                                     .stream()
                                                                     .collect( Collectors.toMap( CrmUser::getIamId, CrmUser::id ) );

                    final Long iamOrgIds = organizationService.getIamOrgId( orgId );

                    final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>()
                    {
                        @Override
                        public void onSuccess( final Collection<User> users )
                        {
                            try
                            {
                                final Map<Long, User> iamUsers = users.stream()
                                                                      .collect( Collectors.toMap( User::id, Function.identity() ) );
                                final Collection<Long> crmUserIds = iamUsers.keySet();
                                final Long currentUserId = LoggedInUserInfo.getUser()
                                                                           .id();

                                CodeBlockExecutor.forEachSafely( crmUserIds,
                                                                 crmUserId ->
                                                                 {
                                                                     final Long userId = userIdsMap.get( crmUserId );

                                                                     if( userJsons.containsKey( userId ) )
                                                                     {
                                                                         final JsonObject userJson = userJsons.get( userId );
                                                                         final User crmUser = iamUsers.get( crmUserId );

                                                                         if( Objects.equals( currentUserId, userId ) )
                                                                         {
                                                                             userJson.put( CRMDashboardKeys.IS_CURRENT_USER, true );
                                                                         }

                                                                         userJson.put( CommonKeys.NAME, crmUser.getFullName() );
                                                                     }
                                                                 } );

                                responseJson.forEach( crmResponse::put );
                            }
                            catch( final Throwable throwable )
                            {
                                crmResponse.setErrorCode( ErrorCode.get( AnalyticsErrorCodes.USER_DISPATCH_FAILED, "i18n.analytics.user.dispatch.failed", throwable));
                            }
                            finally
                            {
                                crmResponse.send();
                            }
                        }

                        @Override
                        public void onFailure( final Throwable throwable )
                        {
                            crmResponse.setErrorCode( ErrorCode.get( AnalyticsErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable ) );
                            crmResponse.send();
                        }
                    };

                    userService.fetchUsers( iamOrgIds , userIdsMap.keySet(), usersFetchCallBack );
                } else {
                    responseJson.forEach(crmResponse::put);
                    crmResponse.send();
                }
            }
            default ->
            {
                throw ErrorCode.get( AnalyticsErrorCodes.INVALID_ACTION_TYPE,
                            "i18n.analytics.invalid.action.type" );
            }
        }
    }
}
