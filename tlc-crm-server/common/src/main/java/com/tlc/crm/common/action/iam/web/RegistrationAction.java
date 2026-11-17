package com.tlc.crm.common.action.iam.web;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.IamRedirectHandler;
import com.tlc.crm.common.queue.CommonQueueTopics;
import com.tlc.iam.client.internal.action.web.CookieBuilder;
import com.tlc.iam.common.ApplicationFields;
import com.tlc.iam.common.action.PublicAction;
import com.tlc.iam.common.action.req.IamHttpRequest;
import com.tlc.iam.common.action.res.IamHttpResponse;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.OrganizationFields;
import com.tlc.iam.common.models.ClientApplication;
import com.tlc.iam.common.models.converter.ApplicationConverter;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.ProducerStore;
import com.tlc.web.HttpStatus;
import com.tlc.web.HttpUtil;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Handles the user signup related actions
 * </p>
 *
 * @author  Mohammed Asif J.
 * @version 1.0
 */
abstract class RegistrationAction extends PublicAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationAction.class);

    protected final EventNotifier eventNotifier;
    protected final IamRedirectHandler redirectHandler;
    protected final Configurations iamConfigurations;

    private final ProducerStore producerStore;
    private final Configurations webConfigurations;
    private final ApplicationConverter applicationConverter;
    private final SubscriptionTopic orgInitiatorTopic;

    protected RegistrationAction() {
        this.eventNotifier = EventNotifier.get();
        this.redirectHandler = IamRedirectHandler.getInstance();
        this.producerStore = ProducerStore.getInstance();
        this.webConfigurations = ConfHolder.getInstance().getProperties(ApplicationFields.WEB);
        this.iamConfigurations = ConfHolder.getInstance().getProperties(IamFields.IAM);
        this.applicationConverter = ApplicationConverter.getInstance();
        this.orgInitiatorTopic = CommonQueueTopics.getOrgInitiatorTopic(QueueModel.Type.KAFKA);
    }

    /**
     * <p>
     * Registers an user
     * </p>
     *
     * @param dataJson           Represents the {@link JsonObject} with user data
     * @param request            Represents the {@link IamHttpRequest}
     * @param response           Represents the {@link IamHttpResponse}.
     * @param isRedirectRequired Represents whether redirection required or not
     */
    protected void register(final JsonObject dataJson, final IamHttpRequest request,
                            final IamHttpResponse response, final boolean isRedirectRequired) {
        final EventCallback<byte[]> orgCreatedCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] orgBytes) {
                final JsonObject orgJson = Json.object(orgBytes);

                if (orgJson.containsKey(IamFields.ERRORS)) {
                    final JsonArray errors = orgJson.getJsonArray(IamFields.ERRORS);
                    final List<String> errorsList = new ArrayList<>();
                    final int size = errors.size();

                    for (int index = NumericalConstant.ZERO; index < size; index++) {
                        errorsList.add(errors.getString(index));
                    }

                    response.put(IamFields.ERRORS, response.resolveI18nString(errorsList));
                    sendError(response, HttpStatus.BAD_REQUEST,
                            ErrorCode.get(IamErrorCodes.SIGNUP_FAILED, errorsList.get(NumericalConstant.ZERO)));
                } else {
                    final String applicationName = dataJson.getString(IamFields.APPLICATION);
                    final EventCallback<byte[]> applicationFetchEventCallback = new EventCallback<>() {

                        @Override
                        public void onSuccess(final byte[] data) {
                            try {
                                final JsonObject applicationJson = Json.object(data)
                                        .put(ApplicationFields.COOKIE_DOMAIN, webConfigurations.getConfig(ApplicationFields.COOKIE_DOMAIN))
                                        .put(ApplicationFields.SAME_SITE_MODE, ApplicationFields.NONE);
                                final ClientApplication application = applicationConverter.convertToApplication(applicationJson);
                                final String accessToken = orgJson.getString(IamFields.ACCESS_TOKEN);

                                response.addCookies(CookieBuilder.buildAuthCookie(application, accessToken));
                                applicationJson.put(ApplicationFields.COOKIE_DOMAIN, StringUtil.EMPTY_STRING);
                                response.addCookies(CookieBuilder.buildAuthCookie(applicationConverter.convertToApplication(applicationJson), accessToken));
                                response.addCookies(CookieBuilder.buildDevAuthCookie(application, accessToken));

                                if (isRedirectRequired) {
                                    redirectHandler.handleClientRedirect(request, response,
                                            HttpUtil.buildAbsoluteUrl(iamConfigurations.getConfig(IamFields.REDIRECT_URL_SERVER_COMPLETE_SIGNUP)),
                                            iamConfigurations.getConfig(IamFields.REDIRECT_URL_LOCAL_COMPLETE_SIGNUP)
                                    );
                                } else {
                                    response.put(IamFields.MESSAGE, response.resolveI18nString("i18n.iam.accounts.org.created"));
                                    response.put(OrganizationFields.ID, orgJson.getLong(OrganizationFields.ID));
                                }
                                orgJson.put(OrganizationFields.TIME_ZONE, request.timeZone().getId());
                                orgJson.put(OrganizationFields.LANGUAGE, request.locale().getDisplayLanguage());
                                producerStore.getProducer(orgInitiatorTopic).ifPresent(producer ->
                                        producer.send(new StreamMessage<>(orgJson.getString(OrganizationFields.ID), orgInitiatorTopic, orgJson.getBytes())));
                                response.send();
                            } catch (final Throwable throwable) {
                                sendError(response, null, throwable);
                            }
                        }

                        @Override
                        public void onFailure(final Throwable cause) {
                            LoggerUtil.logError(LOGGER, cause, "SignUp: Invalid application name, Application Name: {}, Reason : {}", applicationName, cause.getMessage());
                            sendError(response, null, ErrorCode.get(IamErrorCodes.INVALID_APP_NAME, "i18n.invalid.app.name", cause));
                        }
                    };

                    eventNotifier.send(new Event<>(IamEvents.APPLICATION_FETCH, applicationName), applicationFetchEventCallback);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                final String message = throwable.getMessage();

                LoggerUtil.logError(LOGGER, throwable, "SignUp: Organization create failed, Reason : {}", message);
                sendError(response, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.get(IamErrorCodes.SIGNUP_FAILED,
                        message.contains(IamErrorCodes.PHONE_OR_EMAIL_ALREADY_EXISTS.name()) ?
                                "i18n.iam.accounts.email.or.phone.already.exists" : "i18n.iam.accounts.org.creation.failed"));
            }
        };

        eventNotifier.send(new Event<>(IamEvents.CREATE_ORGANIZATION, dataJson.getBytes()), orgCreatedCallback);
    }
}
