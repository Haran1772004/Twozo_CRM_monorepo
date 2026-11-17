package com.tlc.crm.core.action.analytics;

import com.tlc.analytics.config.action.AnalyticsRequest;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.commons.code.ErrorCodeUtil;

import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMViewType;
import com.tlc.crm.core.internal.status.CoreErrorCodes;

import com.tlc.i18n.I18nResolver;

import com.tlc.iam.common.models.User;

import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.web.client.event.EventCallback;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages Reports Action related web actions
 * </p>
 *
 * @author James
 * @version 1.0
 * @see ReportsAction
 */

public abstract class ReportsAction extends AuthorizationController
{
    private static final Logger LOGGER = LoggerFactory.getLogger( ReportsAction.class );

    private final UserService userService;
    private final com.tlc.iam.client.UserService iamUserService;
    private final OrganizationService organizationService;

    protected ReportsAction()
    {
        userService = UserService.getInstance();
        iamUserService = com.tlc.iam.client.UserService.getInstance();
        organizationService = OrganizationService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  The request object representing a {@link CrmRequest}
     * @param crmResponse The {@link CrmResponse} to send back to the client
     */
    @Override
    public final void authorizedProcess( final @NonNull CrmRequest crmRequest,
                                         final @NonNull CrmResponse crmResponse ) throws Exception
    {

        final JsonObject requestJson    = crmRequest.getRequestJson();
        final Long orgId                = crmRequest.orgId();
        final Long userId               = crmRequest.userId();
        final String type               = requestJson.getString( "type" );
        final ActionType actionType     = ActionType.getActionType( type );
        final JsonObject data           = requestJson.getJsonObject( "data" );
        final I18nResolver i18nResolver = crmRequest.i18nResolver();
        final AnalyticsRequest analyticsRequest = new AnalyticsRequest( orgId, userId, crmRequest.getUser().getRoleId(),
                                                                        crmRequest.timeZone(), crmResponse );

        crmResponse.convertIntoAsync();

        final Consumer<Exception> completionHandler = (validationError) -> {
            try
            {
                if( validationError != null )
                {
                    throw validationError;
                }

                processRequest(analyticsRequest, actionType, data, i18nResolver);
            }
            catch ( final Exception error )
            {
                final ErrorCode errorCode = ErrorCodeUtil.getOrDefaultErrorCode( error );

                LOGGER.error("Stack: ", error);

                crmResponse.setErrorCode( errorCode );
                crmResponse.setErrorSummary( new ErrorSummary( new ErrorDescriptor( errorCode ) ) );
                crmResponse.send();
            }
        };

        validateRequest( orgId, userId, data, actionType, completionHandler );

        LOGGER.info("CRM Analytics [REQUEST-TRACE] : caller thread released | action={} | thread=[{}]", actionType, Thread.currentThread().getName());
    }

    /**
     * Validates the incoming request based on the organization ID, user ID, data, and action type.
     *
     * @param orgId      It represents the organization ID
     * @param userId     It represents the user ID
     * @param data       The JsonObject containing the request data, must not be null
     * @param actionType The type of action being performed, must not be null
     * @throws Exception if the validation fails or any error occurs during validation
     */
    abstract protected void validateRequest( final @NonNull Long orgId,
                                             final @NonNull Long userId,
                                             final @NonNull JsonObject data,
                                             final @NonNull ActionType actionType ) throws Exception;

    /**
     * Async version of validateRequest that accepts a completion consumer.
     * The consumer is called with {@code null} on success, or with the exception on failure.
     * Subclasses can override this to provide fully async validation.
     * Default implementation delegates to the sync version.
     *
     * @param orgId              It represents the organization ID
     * @param userId             It represents the user ID
     * @param data               The JsonObject containing the request data
     * @param actionType         The type of action being performed
     * @param completionHandler  Called with null on success, or Exception on failure
     */
    protected void validateRequest( final @NonNull Long orgId,
                                    final @NonNull Long userId,
                                    final @NonNull JsonObject data,
                                    final @NonNull ActionType actionType,
                                    final @NonNull Consumer<Exception> completionHandler )
    {
        try
        {
            validateRequest( orgId, userId, data, actionType );
            completionHandler.accept( null );
        }
        catch ( final Exception exception )
        {
            completionHandler.accept( exception );
        }
    }

    /**
     * Processes the analytics request for the given action type and writes the response
     * directly to the {@link com.tlc.crm.common.action.CrmResponse} held within the
     * {@link AnalyticsRequest}. Implementations are responsible for sending the response.
     *
     * @param analyticsRequest the analytics request context, including org, user, and response handle
     * @param actionType       the type of action being performed, must not be null
     * @param requestData      the JSON payload containing request-specific data, must not be null
     * @param i18nResolver     an instance of {@link I18nResolver} for handling internationalization
     */
    abstract protected void processRequest(final @NonNull AnalyticsRequest analyticsRequest,
                                           final @NonNull ActionType actionType,
                                           final @NonNull JsonObject requestData,
                                           final @NonNull I18nResolver i18nResolver);

    /**
     * <p>
     * Fetch and dispatch the reports users
     * </p>
     *
     * @param orgId             Represents an organisation id
     * @param userIds           Contains the collection of user ids
     * @param data              Represents th report data
     * @param crmResponse       Represents the {@link CrmResponse} details
     * @param viewType          Represents viewType of the Chart
     * @param i18nResolver      an instance of {@link I18nResolver} for handling internationalization
     * @param convertibleTypes  a JsonArray containing convertible types
     */
    protected void fetchAndDispatchUser(final @NonNull Long orgId,
                                        final @NonNull Collection<Long> userIds,
                                        final @NonNull Object data,
                                        final @NonNull CrmResponse crmResponse,
                                        final CRMViewType viewType,
                                        final @NonNull I18nResolver i18nResolver,
                                        final JsonArray convertibleTypes)
    {
        final Map<Long, CrmUser> crmUserMap = userService.getUsers( orgId, userIds )
                                                         .stream()
                                                         .collect( Collectors.toMap( CrmUser::id,
                                                                                     Function.identity() ) );
        final Collection<Long> iamUserIds = crmUserMap.values()
                                                      .stream()
                                                      .map( CrmUser::getIamId )
                                                      .toList();
        final Long iamOrgId = organizationService.getIamOrgId( orgId );

        crmResponse.convertIntoAsync();
        final EventCallback<Collection<User>> userEventCallback = new EventCallback<>()
        {
            @Override
            public void onSuccess( final @NonNull Collection<User> users )
            {
                final Map<Long, User> userMap = users.stream()
                                                     .collect( Collectors.toMap( User::id,
                                                                                 Function.identity() ) );

                crmUserMap.forEach( ( userId, crmUser ) ->
                           {
                               crmUser.setUser( userMap.get( crmUser.getIamId() ) );
                           } );

                getJsonResponse( orgId, crmUserMap, data, viewType, i18nResolver ).forEach( crmResponse::put );

                if(Objects.nonNull(convertibleTypes)) {
                    crmResponse.put(CRMViewPropertyKeys.CONVERTIBLE_VIEW_TYPE, convertibleTypes);
                }

                crmResponse.send();
            }

            @Override
            public void onFailure( Throwable throwable )
            {
                sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.USER_RETRIEVAL_FAILED, "i18n.iam.user.retrieval.failed", throwable));
            }
        };
        iamUserService.fetchUsers( iamOrgId, iamUserIds, userEventCallback );
    }

    /**
     * <p>
     * Gets the {@link JsonObject} response based on the given model
     * </p>
     *
     * @param orgId        Represents an organisation id
     * @param crmUserMap   Contains the collection of crm users
     * @param data         Represents the data
     * @param i18nResolver Represents the {@link I18nResolver}
     * @return The {@link JsonObject} response
     */
    @NonNull
    protected JsonObject getJsonResponse( final @NonNull Long orgId,
                                          final @NonNull Map<Long, CrmUser> crmUserMap,
                                          final @NonNull Object data,
                                          final CRMViewType viewType,
                                          final @NonNull I18nResolver i18nResolver )
    {
        return Json.object();
    }
}
