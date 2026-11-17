package com.tlc.crm.entity.iam.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Env;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.preferences.model.RegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.holder.OrgPreferenceHolder;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.crm.common.iam.api.preferences.model.impl.OrgRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserNotificationPreference;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.handler.OrgInitializationStatusHandler;
import com.tlc.crm.common.iam.handler.OrgInitializationStatusHandler.InternalOrgInitializationStatus;
import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.iam.field.preferences.PreferenceFields;
import com.tlc.crm.common.iam.internal.CrmUserEntity;
import com.tlc.crm.common.iam.internal.manager.OrganizationManager;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.models.Language;
import com.tlc.crm.common.models.NotificationState;
import com.tlc.crm.entity.currency.api.model.BaseCurrency;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.iam.manager.OrgSignupUpdateManager;
import com.tlc.crm.entity.api.iam.model.OrgForecastCategoryPreference;
import com.tlc.crm.entity.fields.OrgOnboardFields;
import com.tlc.iam.client.internal.action.web.CookieBuilder;
import com.tlc.iam.client.internal.action.SecureAction;
import com.tlc.iam.common.action.req.IamHttpRequest;
import com.tlc.iam.common.action.res.IamHttpResponse;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.OrganizationFields;
import com.tlc.iam.common.models.ClientApplication;
import com.tlc.iam.common.models.Organization;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.converter.ApplicationConverter;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.queue.QueueModel;
import com.tlc.queue.SubscriptionTopicHolder;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.service.QueueService;
import com.tlc.queue.service.provider.QueueServiceProvider;
import com.tlc.web.HttpStatus;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Objects;

/**
 * Manages {@link Organization} related web actions
 *
 * @author Selvakumar
 * @version 1.0
 */
@WebAction(path = "/accounts/organization")
public class OrgManagement extends SecureAction {

    private static final String ORG_DELETE_FAILED_I18N_KEY = "i18n.iam.accounts.org.delete.failed";
    private static final String CORE_CONF_KEY = "core";
    private static final String ORG_ONBOARDING_CONF_KEY = "org-onboarding";
    private static final String ORG_ONBOARDING_EVENT = "ORG_ONBOARDING_EVENT";
    private final static Logger LOGGER = LoggerFactory.getLogger(OrgManagement.class);

    private final OrgSignupUpdateManager orgSignupUpdateManager;
    private final OrganizationManager organizationManager;
    private final OrganizationService organizationService;
    private final OrgInitializationStatusHandler orgInitializationStatusHandler;
    private final QueueService queueService;

    public OrgManagement() {
        this.orgSignupUpdateManager = OrgSignupUpdateManager.getInstance();
        this.organizationManager = OrganizationManager.getInstance();
        this.organizationService = OrganizationService.getInstance();
        this.orgInitializationStatusHandler = OrgInitializationStatusHandler.getInstance();
        this.queueService = QueueServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sProcess(final IamHttpRequest request, final IamHttpResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(IamFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(IamFields.DATA);

        switch (type) {
            case "get" -> {
                //TODO this can replaced by loggedInUserInfo api
                final Event<byte[]> orgFetchEvent = new Event<>(IamEvents.FETCH_ORGANIZATION, data.getBytes());
                final EventCallback<byte[]> callback = new EventCallback<>() {

                    @Override
                    public void onSuccess(final byte[] orgBytes) {

                        if (Objects.isNull(orgBytes)) {
                            sendError(response, null, ErrorCode.get(IamErrorCodes.ORG_NOT_FOUND, "i18n.iam.accounts.org.not.found"));
                        } else {
                            final JsonObject jsonObject = Json.object(orgBytes);

                            response.put(IamFields.MESSAGE, response.resolveI18nString("i18n.iam.accounts.org.retrieval.success"));
                            response.put(IamFields.DATA, jsonObject);
                            response.send();
                        }
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        LOGGER.error("Org retrieval action failed, Reason : {}", throwable.getMessage());
                        sendError(response, null, ErrorCode.get(IamErrorCodes.OPERATION_FAILED, "i18n.iam.accounts.org.retrieval.failed"));
                    }
                };

                EventNotifier.get().send(orgFetchEvent, callback);
            }
            case "update" -> {
                final User user = request.getUser();

                if (!user.isAdminUser()) {
                    sendError(response, HttpStatus.FORBIDDEN, ErrorCode.get(IamErrorCodes.ACCESS_DENIED, "i18n.iam.accounts.org.update.access.denied"));
                }

                data.put(OrganizationFields.USER_ID, user.id());
                final Event<byte[]> orgUpdateEvent = new Event<>(IamEvents.ORGANIZATION_UPDATE, data.getBytes());
                final EventCallback<byte[]> orgUpdateCallback = new EventCallback<>() {

                    @Override
                    public void onSuccess(final byte[] data) {
                        try {
                            if (Objects.isNull(data)) {
                                sendError(response, HttpStatus.BAD_REQUEST, ErrorCode.get(IamErrorCodes.ORG_UPDATE_FAILED, "i18n.iam.accounts.org.update.failed"));
                            } else {
                                final JsonObject jsonObject = Json.object(data);

                                if (jsonObject.containsKey(IamFields.MESSAGE)) {
                                    response.put(IamFields.MESSAGE, Objects.equals(IamFields.SUCCESS, jsonObject.getString(IamFields.MESSAGE))
                                            ? response.resolveI18nString("i18n.iam.accounts.org.update.success")
                                            : response.resolveI18nString("i18n.iam.accounts.org.update.failed"));
                                } else if (jsonObject.containsKey(IamFields.ERRORS)) {
                                    final JsonArray errors = jsonObject.getJsonArray(CrmUserFields.ERRORS.getName());

                                    sendError(response, null, ErrorCode.get(IamErrorCodes.ORG_UPDATE_FAILED, errors.getString(0)));
                                }
                            }
                        } finally {
                            response.send();
                        }
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        LOGGER.error("Org update failed, Reason : {}", throwable.getMessage());
                        sendError(response, HttpStatus.BAD_REQUEST, ErrorCode.get(IamErrorCodes.ORG_UPDATE_FAILED, "i18n.iam.accounts.org.update.failed"));
                    }
                };
                EventNotifier.get().send(orgUpdateEvent, orgUpdateCallback);
            }
            case "delete" -> {
                final User user = request.getUser();
                final JsonObject jsonObject = Json.object();

                jsonObject.put(IamFields.ID, user.orgId());
                final Event<byte[]> event = new Event<>(IamEvents.CHECK_ORG_EXISTENCE, jsonObject.getBytes());
                final EventCallback<Boolean> orgExistenceCallback = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Boolean orgExists) {
                        if (orgExists) {
                            if (user.getType() == User.Type.ORG_ADMIN) {
                                final Event<byte[]> orgStateEvent = new Event<>(IamEvents.ORG_STATE_MODIFIER,
                                        jsonObject.put(OrganizationFields.IS_ACTIVE, false).getBytes());
                                final EventCallback<Boolean> orgStateCallback = new EventCallback<>() {

                                    @Override
                                    public void onSuccess(final Boolean isUpdated) {
                                        if (isUpdated) {
                                            final JsonObject logoutData = Json.object();

                                            logoutData.put(IamFields.ACCESS_TOKEN, request.cookie(IamFields.ACCESS_TOKEN).getValue());

                                            final Event<byte[]> logoutEvent = new Event<>(IamEvents.SIGN_OUT, logoutData.getBytes());
                                            final EventCallback<Boolean> logoutCallback = new EventCallback<>() {

                                                @Override
                                                public void onSuccess(final Boolean logoutSuccess) {
                                                    response.put(IamFields.MESSAGE, response.resolveI18nString("i18n.iam.accounts.org.delete.success"));
                                                    doLogout(request, response);
                                                }

                                                @Override
                                                public void onFailure(Throwable throwable) {
                                                    LOGGER.error("Logout action failed. App : {}, Error : {}", request.getParameter(IamFields.APPLICATION), throwable.getMessage());
                                                    doLogout(request, response);
                                                }
                                            };

                                            EventNotifier.get().send(logoutEvent, logoutCallback);
                                        } else {
                                            response.setErrorCode(ErrorCode.get(IamErrorCodes.ORG_DELETE_FAILED, "i18n.iam.accounts.org.delete.failed"));
                                            response.send();
                                        }
                                    }

                                    @Override
                                    public void onFailure(final Throwable throwable) {
                                        LOGGER.error("Org delete action failed, Reason : {}", throwable.getMessage());
                                        sendError(response, null, ErrorCode.get(IamErrorCodes.ORG_DELETE_FAILED, ORG_DELETE_FAILED_I18N_KEY, throwable));
                                    }
                                };

                                EventNotifier.get().send(orgStateEvent, orgStateCallback);
                            } else {
                                response.setErrorCode(ErrorCode.get(IamErrorCodes.AUTHORIZATION_FAILED, "i18n.iam.accounts.org.cancellation.restricted"));
                                response.send();
                            }
                        } else {
                            sendError(response, null, ErrorCode.get(IamErrorCodes.ORG_NOT_FOUND, "i18n.iam.accounts.org.not.found"));
                        }
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        LOGGER.error("Org delete action failed, Reason : {}", throwable.getMessage());
                        sendError(response, null, ErrorCode.get(IamErrorCodes.ORG_DELETE_FAILED, ORG_DELETE_FAILED_I18N_KEY, throwable));
                    }
                };

                EventNotifier.get().send(event, orgExistenceCallback);
            }
            case "completeSignup" -> completeSignup(request, data, response);
        }
    }

    /**
     * Completes the final stage of the organization signup process.
     *
     * @param request  The incoming HTTP request containing context or session data.
     * @param data     The parsed JSON payload with signup-related information.
     * @param response The HTTP response object used to send the final result back to the client.
     */
    private void completeSignup(final IamHttpRequest request, final JsonObject data, final IamHttpResponse response) {
        final User user = request.getUser();
        final Long iamOrgId = user.orgId();

        data.put(IamFields.ID, iamOrgId);
        data.put(IamFields.USER_ID, user.id());
        final CrmOrg crmOrg = organizationService.getCrmOrg(organizationService.getOrgId(user.orgId()));

        if (crmOrg.getInitializationStatus() == CrmOrg.InitializationStatus.COMPLETED) {
            sendError(response, HttpStatus.OK, ErrorCode.get(IamErrorCodes.ORG_UPDATE_FAILED, "i18n.iam.accounts.org.already.initialized"));
        } else {
            final Event<byte[]> orgSignupUpdateEvent = new Event<>(IamEvents.ORG_SIGNUP_UPDATE, data.getBytes());
            final EventCallback<byte[]> callback = new EventCallback<>() {

                /**
                 * {@inheritDoc}
                 *
                 * @param result Response from the consumer.
                 */
                @Override
                public void onSuccess(final byte[] result) {
                    try {
                        if (Objects.isNull(result)) {
                            sendError(response, HttpStatus.BAD_REQUEST, ErrorCode.get(IamErrorCodes.ORG_UPDATE_FAILED, "i18n.iam.accounts.org.update.failed"));
                        } else {
                            final JsonObject jsonObject = Json.object(result);

                            if (jsonObject.containsKey(IamFields.MESSAGE)) {

                                if (Objects.equals(IamFields.SUCCESS, jsonObject.getString(IamFields.MESSAGE))) {
                                    updateSignupDetails(request, iamOrgId, user.id(), data);
                                    response.put(IamFields.MESSAGE, response.resolveI18nString("i18n.iam.accounts.org.update.success"));
                                } else {
                                    response.setErrorCode(ErrorCode.get(IamErrorCodes.ORG_UPDATE_FAILED));
                                    response.put(IamFields.MESSAGE, response.resolveI18nString("i18n.iam.accounts.org.update.failed"));
                                }
                                response.send();
                            } else if (jsonObject.containsKey(IamFields.ERRORS)) {
                                final JsonArray errors = jsonObject.getJsonArray(CrmUserFields.ERRORS.getName());

                                sendError(response, null, ErrorCode.get(IamErrorCodes.ORG_UPDATE_FAILED, errors.getString(0)));
                            }
                        }
                    } catch (Throwable throwable) {
                        sendError(response, null, ErrorCode.get(IamErrorCodes.ORG_UPDATE_FAILED, throwable));
                    }
                }

                /**
                 * {@inheritDoc}
                 *
                 * @param throwable Cause of the failure.
                 */
                @Override
                public void onFailure(final Throwable throwable) {
                    sendError(response, HttpStatus.BAD_REQUEST, ErrorCode.get(IamErrorCodes.ORG_UPDATE_FAILED, "i18n.iam.accounts.org.update.failed"));
                }
            };

            EventNotifier.get().send(orgSignupUpdateEvent, callback);
        }
    }

    /**
     * Does the logout action
     *
     * @param request  Http request
     * @param response Http response
     */
    private void doLogout(final IamHttpRequest request, final IamHttpResponse response) {
        final ApplicationConverter applicationConverter = ApplicationConverter.getInstance();
        final Event<String> applicationFetchEvent = new Event<>(IamEvents.APPLICATION_FETCH, "crm");
        final EventCallback<byte[]> applicationFetchCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] data) {
                try {
                    final ClientApplication clientApplication = applicationConverter.convertToApplication(Json.object(data));

                    response.addCookies(CookieBuilder.buildAuthCookie(clientApplication, ""));

                    if (Env.isDevMode()) {
                        response.addCookies(CookieBuilder.buildDevAuthCookie(clientApplication, ""));
                    }
                } finally {
                    response.send();
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                LOGGER.error("Invalid application name, Reason : {}", cause.getMessage());
                response.setErrorCode(ErrorCode.get(IamErrorCodes.INVALID_APP_NAME));
                response.send();
            }
        };

        EventNotifier.get().send(applicationFetchEvent, applicationFetchCallback);
    }

    /**
     * Updates organization signup details using the provided organization ID and data.
     *
     * @param iamOrgId The unique identifier of the organization.
     * @param data     A JsonObject containing the updated signup information.
     */
    private void updateSignupDetails(final IamHttpRequest request, final long iamOrgId, final long iamUserId, final JsonObject data) {
        final OrgRegionalPreference orgRegionalPreference = new OrgRegionalPreference();
        final UserRegionalPreference userRegionalPreference = new UserRegionalPreference();
        final CrmOrg crmOrg = new CrmOrg();
        final CrmUserEntity crmUser = new CrmUserEntity();
        final String timezoneKey = PreferenceFields.TIME_ZONE.getName();
        final TimezoneInfo timezoneInfo = data.containsKey(timezoneKey) ? TimezoneInfo.getTimezoneById(data.getInt(timezoneKey)) : TimezoneInfo.UTC;
        final Language language = Language.get(request.locale().getDisplayLanguage());
        final OrgForecastCategoryPreference orgForecastCategoryPreference = new OrgForecastCategoryPreference();

        orgForecastCategoryPreference.setEnabled(true);
        orgRegionalPreference.setLanguage(language);
        orgRegionalPreference.setDateFormat(RegionalPreference.DateFormat.RELATIVE_DATE);
        orgRegionalPreference.setTimezoneInfo(timezoneInfo);
        orgRegionalPreference.setTimeFormat(RegionalPreference.TimeFormat.TWENTY_FOUR_HOURS);

        crmOrg.addPreference(orgForecastCategoryPreference);
        crmOrg.addPreference(orgRegionalPreference);
        crmOrg.setIamOrgId(iamOrgId);

        userRegionalPreference.setLanguage(language);
        userRegionalPreference.setDateFormat(RegionalPreference.DateFormat.RELATIVE_DATE);
        userRegionalPreference.setTimezoneInfo(timezoneInfo);
        userRegionalPreference.setTimeFormat(RegionalPreference.TimeFormat.TWENTY_FOUR_HOURS);

        crmUser.addPreference(userRegionalPreference);
        crmUser.addPreference(new UserNotificationPreference(NotificationState.ON));

        final long crmOrgId = OrganizationService.getInstance().getOrgId(iamOrgId);
        final OrgPreferenceHolder orgPreferenceHolder = crmOrg.getPreferenceHolder();
        final UserPreferenceHolder userPreferenceHolder = crmUser.getPreferenceHolder();
        final BaseCurrency baseCurrency = new BaseCurrency();

        baseCurrency.setOrgId(crmOrgId);
        baseCurrency.setImmutable(false);
        baseCurrency.setDefaultCurrency(Currency.getCurrencyById(data.getShort(FieldKeys.CURRENCY.getName())));
        baseCurrency.setDecimalPoint(BaseCurrency.DecimalPoint.get(BaseCurrency.DecimalPoint.TWO.id()));

        orgPreferenceHolder.setOrgId(crmOrgId);

        userPreferenceHolder.setOrgId(crmOrgId);
        userPreferenceHolder.setUserId(UserService.getInstance().getUserId(crmOrgId, iamUserId));

        try {
            orgSignupUpdateManager.update(crmOrgId, orgPreferenceHolder, userPreferenceHolder, baseCurrency);
            orgInitializationStatusHandler.handleSafely(iamOrgId, internalOrgInitializationStatus -> {
                switch (internalOrgInitializationStatus) {
                    case INTERNAL_PROCESS_DONE, FINISHED -> {
                        orgInitializationStatusHandler.setStatus(iamOrgId, InternalOrgInitializationStatus.FINISHED);
                        organizationManager.updateInitializationStatus(crmOrgId, CrmOrg.InitializationStatus.COMPLETED);
                    }
                    default -> {
                        orgInitializationStatusHandler.setStatus(iamOrgId, InternalOrgInitializationStatus.SIGNUP_PROCESS_DONE);
                        organizationManager.updateInitializationStatus(crmOrgId, CrmOrg.InitializationStatus.SIGNUP_COMPLETED);
                    }
                }
            });
            publishOrgOnboardingEvent(iamOrgId, iamUserId, crmOrgId, data);
        } catch (Throwable throwable) {
            orgInitializationStatusHandler.handleSafely(iamOrgId, ignored -> {
                orgInitializationStatusHandler.setStatus(iamOrgId, InternalOrgInitializationStatus.FAILED);
                organizationManager.updateInitializationStatus(crmOrgId, CrmOrg.InitializationStatus.FAILED);
            });

            LOGGER.error("Signup: Complete signup action failed. Org ID: {}, Reason: {} ", crmOrgId, throwable.getMessage());
            throw ErrorCode.get(IamErrorCodes.ORG_UPDATE_FAILED, throwable);
        }
    }

    /**
     * <p>
     * Publishes the org onboarding event.
     * </p>
     *
     * @param iamOrgId  Represents the IAM org id.
     * @param iamUserId Represents the IAM user id.
     * @param crmOrgId  Represents the CRM org id.
     * @param data      Holds the signup form data.
     */
    private void publishOrgOnboardingEvent(final long iamOrgId, final long iamUserId, final long crmOrgId, final JsonObject data) {
        if (!Boolean.parseBoolean(ConfHolder.getInstance().getProperties(CORE_CONF_KEY).getNestedConfigs(ORG_ONBOARDING_CONF_KEY).
                optConfig(OrgOnboardFields.ENABLED, "false"))) {
            return;
        }

        final String requestId = Base64.getEncoder().encodeToString(String.format("%s:%s", iamOrgId, iamUserId).getBytes());
        final SubscriptionTopic topic = SubscriptionTopicHolder.getInstance().getSubscriptionTopic(QueueModel.Type.KAFKA, ORG_ONBOARDING_EVENT);
        final KafkaProducerConfig producerConfig = new KafkaProducerConfig(topic);

        try (final Producer<byte[]> producer = queueService.createProducer(producerConfig)) {
            final JsonObject payload = Json.object()
                    .put(OrgOnboardFields.IAM_ORG_ID, iamOrgId)
                    .put(OrgOnboardFields.IAM_USER_ID, iamUserId)
                    .put(OrgOnboardFields.ORG_ID, crmOrgId)
                    .put(OrgOnboardFields.SIGNUP_DATA, data);

            producer.send(new StreamMessage<>(requestId, topic, payload.getBytes()));
        } catch (final Throwable throwable) {
            LOGGER.error("Org onboard action: Failed to publish event for IAM Org ID: {}, IAM User ID: {}, Reason: {}",
                    iamOrgId, iamUserId, throwable.getMessage());
        }
    }
}
