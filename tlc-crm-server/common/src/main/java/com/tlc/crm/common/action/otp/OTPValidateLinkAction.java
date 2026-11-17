package com.tlc.crm.common.action.otp;

import com.tlc.commons.json.Json;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.Env;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.fields.QueryParamAttributes;
import com.tlc.crm.common.models.CommonFields;
import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.queue.CommonQueueTopics;
import com.tlc.iam.client.internal.action.web.CookieBuilder;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.OrganizationFields;
import com.tlc.iam.common.models.ClientApplication;
import com.tlc.iam.common.models.converter.ApplicationConverter;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.iam.common.action.PublicAction;
import com.tlc.iam.common.action.req.IamHttpRequest;
import com.tlc.iam.common.action.res.IamHttpResponse;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.otp.manager.OTPRequestManager;
import com.tlc.otp.model.Category;
import com.tlc.otp.model.Channel;
import com.tlc.otp.model.OTPRequest;
import com.tlc.otp.service.OTPValidator;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.service.provider.QueueServiceProvider;
import com.tlc.web.HttpStatus;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpUtil;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Handles verification of otp link
 * </p>
 *
 * @author Kishore
 * @version 1.0
 */
@WebAction(path = "/otp/validate", method = HttpMethod.GET)
public class OTPValidateLinkAction extends PublicAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(OTPValidateLinkAction.class);

    private final OTPValidator otpValidator;
    private final Map<String, String> urls;
    private final OTPRequestManager otpRequestManager;

    public OTPValidateLinkAction() {
        urls = new HashMap<>();
        otpValidator = OTPValidator.getInstance();
        otpRequestManager = OTPRequestManager.getInstance();
        final Configurations configurations = ConfHolder.getInstance()
                .getProperties("crm-common").getNestedConfigs("channelOTPVerification.accountVerification.email");
        final Configurations redirectUrlConfigurations = configurations.getNestedConfigs("redirectUrl");

        urls.put(CommonFields.SIGN_UP, redirectUrlConfigurations.getConfig(CommonFields.SIGN_UP));
        urls.put(CommonFields.LOGIN, redirectUrlConfigurations.getConfig(CommonFields.LOGIN));
        urls.put(CommonFields.LINK_EXPIRED, redirectUrlConfigurations.getConfig(CommonFields.LINK_EXPIRED));
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Http request
     * @param response Http response
     * @throws Exception
     */
    @Override
    public void pProcess(final IamHttpRequest request, final IamHttpResponse response) throws Exception {
        final JsonObject tokenJson = Json.object(Base64.getDecoder().decode(request.getParameter(QueryParamAttributes.TOKEN)));
        final String category = tokenJson.getString(CommonFields.CATEGORY);

        switch (category) {
            case CommonFields.ACCOUNT_VERIFICATION -> {
                final EventCallback<Boolean> emailExistenceCallback = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Boolean isEmailExists) {

                        try {
                            if (isEmailExists) {
                                response.setTemporaryRedirectUrl(HttpUtil.buildAbsoluteUrl(urls.get(CommonFields.LOGIN)));
                                response.send();
                            } else {
                                final Optional<Category> accountVerificationCategory = otpRequestManager.getCategory(CommonFields.CRM, category);

                                if (accountVerificationCategory.isPresent()) {
                                    final OTPRequest otpRequest = new OTPRequest(tokenJson.getString(UserFields.RECEIVER), Channel.EMAIL);

                                    otpRequest.setOtp(tokenJson.getString(CommonFields.OTP));
                                    otpRequest.setCategory(accountVerificationCategory.get());

                                    if (otpValidator.validate(otpRequest)) {
                                        final JsonObject data = Json.object();
                                        final JsonObject userJson = tokenJson.getJsonObject(IamFields.STATE);

                                        data.put(CrmUserFields.USER.getName(), userJson);
                                        data.put(IamFields.APPLICATION, CommonFields.CRM);
                                        data.put(OrganizationFields.USER_AGENT, request.getUserAgent().toJson());

                                        initializeSignup(request, response, data);
                                    } else {
                                        response.setTemporaryRedirectUrl(HttpUtil.buildAbsoluteUrl(urls.get(CommonFields.LINK_EXPIRED)));
                                        response.send();
                                    }
                                }
                            }
                        } catch (final Exception exception) {
                            LoggerUtil.logError(LOGGER, exception, "Email OTP Action: Failed during OTP link validation");
                            sendError(response, null, exception);
                        }
                    }

                    @Override
                    public void onFailure(final Throwable cause) {
                        LoggerUtil.logError(LOGGER, cause, "Email OTP Action: Failed to verify email address existence");
                        response.send();
                    }
                };
                EventNotifier.get().send(new Event<>(IamEvents.CHECK_EMAIL_EXISTENCE, Json.object().put(UserFields.EMAIL, tokenJson.get(UserFields.RECEIVER)).getBytes()), emailExistenceCallback);
            }
        }
    }

    /**
     * Initializes the signup process for a new user.
     *
     * @param request  The incoming HTTP request containing signup details.
     * @param response The HTTP response used to send data or redirect after signup initialization.
     * @param data     A{@link JsonObject} containing signup-related data.
     */
    private void initializeSignup(final IamHttpRequest request, final IamHttpResponse response, final JsonObject data) {
        final Event<byte[]> orgCreateEvent = new Event<>(IamEvents.CREATE_ORGANIZATION, data.getBytes());
        final EventCallback<byte[]> orgCreatedCallback = new EventCallback<>() {

            /**
             * {@inheritDoc}
             *
             * @param orgBytes Response from the consumer.
             */
            @Override
            public void onSuccess(final byte[] orgBytes) {
                final JsonObject jsonObject = Json.object(orgBytes);

                if (jsonObject.containsKey(IamFields.ERRORS)) {
                    final JsonArray errors = jsonObject.getJsonArray(IamFields.ERRORS);

                    sendError(response, HttpStatus.BAD_REQUEST, ErrorCode.get(IamErrorCodes.SIGNUP_FAILED, errors.getString(0)));
                } else {
                    final Event<String> applicationFetchEvent = new Event<>(IamEvents.APPLICATION_FETCH, CommonFields.CRM);
                    final EventCallback<byte[]> applicationFetchEventCallback = new EventCallback<>() {

                        /**
                         * {@inheritDoc}
                         *
                         * @param data Response from the consumer.
                         */
                        @Override
                        public void onSuccess(final byte[] data) {
                            try {
                                final ClientApplication application = ApplicationConverter.getInstance().convertToApplication(Json.object(data));

                                response.addCookies(CookieBuilder.buildAuthCookie(application, jsonObject.getString(IamFields.ACCESS_TOKEN)));

                                if (Env.isDevMode()) {
                                    response.addCookies(CookieBuilder.buildDevAuthCookie(application, jsonObject.getString(IamFields.ACCESS_TOKEN)));
                                }
                                jsonObject.put(OrganizationFields.TIME_ZONE, request.timeZone().getId());
                                jsonObject.put(OrganizationFields.LANGUAGE, request.locale().getDisplayLanguage());

                                final SubscriptionTopic orgInitiatorTopic = CommonQueueTopics.getOrgInitiatorTopic(QueueModel.Type.KAFKA);
                                final KafkaProducerConfig orgInitiatorProducerConfig = new KafkaProducerConfig(orgInitiatorTopic);

                                try (final Producer<byte[]> orgInitiatorProducer = QueueServiceProvider.get().createProducer(orgInitiatorProducerConfig)) {
                                    orgInitiatorProducer.send(new StreamMessage<>(jsonObject.getString(OrganizationFields.ID), orgInitiatorTopic, jsonObject.getBytes()));
                                }
                                response.setTemporaryRedirectUrl(HttpUtil.buildAbsoluteUrl(urls.get(CommonFields.SIGN_UP)));
                                response.send();
                            } catch (Throwable throwable) {
                                sendError(response, null, throwable);
                            }
                        }

                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        public void onFailure(final Throwable cause) {
                            sendError(response, null, ErrorCode.get(IamErrorCodes.INVALID_APP_NAME, "i18n.invalid.app.name", cause));
                        }
                    };

                    EventNotifier.get().send(applicationFetchEvent, applicationFetchEventCallback);
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void onFailure(final Throwable throwable) {
                final String message = throwable.getMessage();
                final String i18nMessage = message.contains(IamErrorCodes.PHONE_OR_EMAIL_ALREADY_EXISTS.name())
                        ? "i18n.iam.accounts.email.or.phone.already.exists" : "i18n.iam.accounts.org.creation.failed";

                LOGGER.error("Organization create failed, Reason : {}", message);
                sendError(response, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.get(IamErrorCodes.SIGNUP_FAILED, i18nMessage));
            }
        };

        EventNotifier.get().send(orgCreateEvent, orgCreatedCallback);
    }
}
