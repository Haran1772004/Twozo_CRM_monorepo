package com.tlc.crm.common.action.otp;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.email.service.NoReplyMailService;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.CommonFields;
import com.tlc.crm.common.models.EmailOTPRequestHolder;
import com.tlc.email.client.template.mail.EmailNotificationBuilder;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.iam.common.action.PublicAction;
import com.tlc.iam.common.action.req.IamHttpRequest;
import com.tlc.iam.common.action.res.IamHttpResponse;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.DateInterval;
import com.tlc.notification.model.alert.EmailNotifiableModel;
import com.tlc.notification.service.template.EmailTemplateBuildRequest;
import com.tlc.otp.manager.OTPRequestManager;
import com.tlc.otp.model.Category;
import com.tlc.otp.model.CategoryConfig;
import com.tlc.otp.model.Channel;
import com.tlc.otp.model.OTPConfiguration;
import com.tlc.otp.model.OTPRequest;
import com.tlc.otp.service.OTPGenerator;
import com.tlc.otp.service.OTPValidator;
import com.tlc.web.HttpUtil;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Handles the otp action requests
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@WebAction(path = "/otp/mgmt")
public class OTPManagement extends PublicAction {

    private static final byte MINUTES = 30;
    private static final int DEFAULT_OTP_DIGIT = 6;
    private static final Logger LOGGER = LoggerFactory.getLogger(OTPManagement.class);

    private final EmailNotificationBuilder emailNotificationBuilder;
    private final OTPGenerator otpGenerator;
    private final OTPValidator otpValidator;
    private final String otpValidateUrl;
    private final OTPRequestManager otpRequestManager;
    private final NoReplyMailService noReplyMailService;

    public OTPManagement() {
        emailNotificationBuilder = EmailNotificationBuilder.getInstance();
        otpGenerator = OTPGenerator.getInstance();
        otpValidator = OTPValidator.getInstance();
        otpRequestManager = OTPRequestManager.getInstance();
        final Configurations configurations = ConfHolder.getInstance().getProperties("crm-common").getNestedConfigs("channelOTPVerification.accountVerification.email");

        otpValidateUrl = configurations.getConfig("redirectUrl.otpValidate");
        this.noReplyMailService = NoReplyMailService.get();
    }

    /**
     * Handles the signup operation
     *
     * @param iamHttpRequest  Http request
     * @param iamHttpResponse Http response
     */
    @Override
    public void pProcess(final IamHttpRequest iamHttpRequest, final IamHttpResponse iamHttpResponse) {
        final JsonObject requestJson = iamHttpRequest.getRequestJson();
        final String type = requestJson.getString(UserFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(UserFields.DATA);

        switch (type) {
            case "generate" -> {
                switch (Channel.getChannelByType(data.getString(CommonFields.CHANNEL))) {
                    case EMAIL -> processEmail(data, iamHttpResponse, true);
                }
            }
            case "validate" -> {
                switch (Channel.getChannelByType(data.getString(CommonFields.CHANNEL))) {
                    case EMAIL -> processEmail(data, iamHttpResponse, false);
                }
            }
        }
    }

    /**
     * <p>
     * Process the email otp for the provided requests
     * </p>
     *
     * @param data          Holds the data {@link JsonObject}
     * @param iamHttpResponse  Holds the {@link IamHttpResponse}
     * @param isOtpGenerate A flag indicates it is otp generate or validate
     */
    private void processEmail(final JsonObject data, final IamHttpResponse iamHttpResponse, final boolean isOtpGenerate) {
        final String emailAddress = data.getString(CommonFields.RECEIVER);
        final EventCallback<Boolean> emailExistenceCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final Boolean isEmailExists) {
                try {
                    if (isEmailExists) {
                        sendError(iamHttpResponse, null, ErrorCode.get(IamErrorCodes.EMAIL_ADDRESS_ALREADY_EXISTS,
                                "i18n.iam.user.email.already.exists"));
                    } else {
                        final Optional<Category> accountVerificationCategory = OTPRequestManager.getInstance()
                                .getCategory(CommonFields.CRM, CommonFields.ACCOUNT_VERIFICATION);

                        if (accountVerificationCategory.isPresent()) {
                            final Category category = accountVerificationCategory.get();

                            if (isOtpGenerate) {
                                final OTPConfiguration otpConfiguration = new OTPConfiguration(data.optInt(CommonFields.DIGIT, DEFAULT_OTP_DIGIT),
                                        OTPConfiguration.Type.get(data.optString(CommonFields.TYPE, OTPConfiguration.Type.NUMERIC.getName())));
                                final Optional<OTPRequest> existingOTPRequest = otpRequestManager.get(emailAddress,
                                        Channel.EMAIL, category);
                                final CategoryConfig categoryConfig = category.getCategoryConfig();
                                final Counter otpTotalAttempts = new Counter();
                                final OTPRequest otpRequest;
                                boolean isEmailSendingEnabled = true;

                                if (existingOTPRequest.isPresent()) {
                                    otpRequest = existingOTPRequest.get();
                                    final int totalAttempts = otpRequest.getTotalAttempts();

                                    if (totalAttempts >= categoryConfig.getMaxAllowedAttempts()) {
                                        iamHttpResponse.put(CommonFields.RESEND_DURATION, TemporalServiceProvider.get()
                                                .plus(categoryConfig.getLockoutDurationAfterMaxAttempts(), DateInterval.SECONDS));
                                        iamHttpResponse.put(ActionFields.MESSAGE, iamHttpResponse.resolveI18nString("i18n.email.otp.send.limit.reached"));

                                        otpRequest.setTotalAttempts(0);
                                        otpRequestManager.updateTotalAttempts(otpRequest);
                                        isEmailSendingEnabled = false;
                                    }
                                    otpTotalAttempts.setCount(totalAttempts);
                                    otpRequest.setTotalAttempts(otpTotalAttempts.incrementAndGet());
                                } else {
                                    otpRequest = new OTPRequest(emailAddress, Channel.EMAIL);

                                    otpRequest.setTotalAttempts(otpTotalAttempts.incrementAndGet());
                                    otpRequest.setCategory(category);
                                }
                                if (isEmailSendingEnabled) {
                                    otpRequest.setExpiresAt(TemporalServiceProvider.get().plus(MINUTES, DateInterval.MINUTES));
                                    otpGenerator.generate(otpRequest, otpConfiguration);

                                    iamHttpResponse.put(CommonFields.RESEND_DURATION,
                                            TemporalServiceProvider.get().plus(categoryConfig.getResendDuration(), DateInterval.SECONDS));
                                    iamHttpResponse.put(ActionFields.MESSAGE, iamHttpResponse.resolveI18nString("i18n.email.otp.sent"));

                                    sendEmail(otpRequest, data);
                                }
                            } else {
                                final OTPRequest otpRequest = new OTPRequest(emailAddress, Channel.EMAIL);

                                otpRequest.setCategory(category);
                                otpRequest.setOtp(data.getString(CommonFields.OTP));

                                if (otpValidator.validate(otpRequest)) {
                                    iamHttpResponse.put(ActionFields.MESSAGE, iamHttpResponse.resolveI18nString("i18n.email.otp.validation.successful"));
                                } else {
                                    sendError(iamHttpResponse, null, ErrorCode.get(CommonErrorCodes.OTP_VALIDATION_FAILED,
                                            "i18n.email.otp.validation.failed"));
                                }
                            }
                        }
                    }
                } catch (final ErrorCode errorCode) {
                    LoggerUtil.logError(LOGGER, errorCode, errorCode.getMessage());
                    sendError(iamHttpResponse, null, errorCode);
                } finally {
                    iamHttpResponse.send();
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                sendError(iamHttpResponse, null, ErrorCode.get(IamErrorCodes.EMAIL_VERIFICATION_FAILED,
                        "i18n.iam.email.address.verification.failed"));
                LoggerUtil.logError(LOGGER, throwable, "Email OTP Action: Failed to verify email address existence");
            }
        };
        EventNotifier.get().send(new Event<>(IamEvents.CHECK_EMAIL_EXISTENCE, Json.object().put(UserFields.EMAIL, emailAddress).getBytes()), emailExistenceCallback);
    }

    /**
     * <p>
     * Sends an email for the provided {@link OTPRequest}
     * </p>
     *
     * @param otpRequest Represents the {@link OTPRequest}
     * @param data       Represents the json request data.
     */
    private void sendEmail(final OTPRequest otpRequest, final JsonObject data) {
        final String emailAddress = otpRequest.getReceiver();
        final Map<String, EmailNotifiableModel> emailNotifiableModelMap = new HashMap<>();
        final EmailOTPRequestHolder emailOtpRequestHolder = new EmailOTPRequestHolder();

        emailOtpRequestHolder.setOtpRequest(otpRequest);
        emailOtpRequestHolder.setOtpVerificationUrl(buildOtpVerificationUrl(emailAddress, otpRequest, data));
        emailNotifiableModelMap.put(CommonFields.MODEL, emailOtpRequestHolder);

        emailNotificationBuilder.build(EmailNotificationTemplate.SIGNUP_EMAIL_OTP_VERIFICATION.getName(),
                        new EmailTemplateBuildRequest(emailNotifiableModelMap))
                .ifPresent(email -> {
                    final EmailRecipients emailRecipients = new EmailRecipients();

                    emailRecipients.addToRecipient(emailAddress);
                    email.setRecipients(List.of(emailRecipients));

                    noReplyMailService.send(email);
                });
    }

    /**
     * Builds the full OTP verification URL with an email token as a query parameter
     *
     * @param email      The user's email address
     * @param otpRequest Represents the {@link OTPRequest}.
     * @param data       Represents the json request data.
     * @return A complete URL for OTP validation
     */
    private String buildOtpVerificationUrl(final String email, final OTPRequest otpRequest, final JsonObject data) {
        final JsonObject tokenPayload = Json.object();

        tokenPayload.put("receiver", email);
        tokenPayload.put("category", otpRequest.getCategory().getName());
        tokenPayload.put("otp", otpRequest.getOtp());
        tokenPayload.put("state", data.getJsonObject(IamFields.STATE));
        final String encodedToken = Base64.getEncoder().encodeToString(tokenPayload.getBytes());

        return String.format("%s%s", HttpUtil.buildAbsoluteUrl(otpValidateUrl), String.format("?token=%s", encodedToken));
    }
}
