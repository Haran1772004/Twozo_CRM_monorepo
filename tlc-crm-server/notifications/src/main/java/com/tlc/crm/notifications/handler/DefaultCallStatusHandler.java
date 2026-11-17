package com.tlc.crm.notifications.handler;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.notifications.api.call.manager.CallHistoryManager;
import com.tlc.crm.notifications.api.call.manager.PhoneSettingsManager;
import com.tlc.crm.notifications.api.call.model.CallDetails;
import com.tlc.crm.notifications.api.call.model.CallForwardDetails;
import com.tlc.crm.notifications.api.call.model.CallSummary;
import com.tlc.crm.notifications.api.call.model.PhoneSettings;
import com.tlc.crm.notifications.internal.status.NotificationErrorCodes;
import com.tlc.crm.notifications.validator.CallDetailsValidator;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.notification.model.call.CallRecord;
import com.tlc.notification.model.call.CallStatus;
import com.tlc.notification.model.call.CallType;
import com.tlc.notification.model.call.VoiceCall;
import com.tlc.notification.service.notification.VoiceCallService;
import com.tlc.notification.service.provider.NotificationServiceProvider;
import com.tlc.notification.webhook.model.CallStatusWebhookData;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.type.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Handles all call status webhook processing, including both web-based calls
 * and click-to-call scenarios. Manages call lifecycle events, participant assignment,
 * call forwarding, and recording retrieval.
 *
 * @author Sathish
 * @version 1.0
 */
public class DefaultCallStatusHandler extends AbstractCallStatusHandler implements CallStatusHandler, SecureUserTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCallStatusHandler.class);
    private static final String UNDERSCORE = "_";
    private static final int ONE = 1;
    private static final String ZERO = "0";

    private final CallHistoryManager callHistoryManager;
    private final UserService userService;
    private final VoiceCallService voiceCallService;
    private final CallDetailsValidator callDetailsValidator;
    private final PhoneSettingsManager phoneSettingsManager;

    private DefaultCallStatusHandler() {
        this.callHistoryManager = CallHistoryManager.getInstance();
        this.userService = UserService.getInstance();
        this.voiceCallService = NotificationServiceProvider.get().getVoiceCallService();
        this.callDetailsValidator = CallDetailsValidator.getInstance();
        this.phoneSettingsManager = PhoneSettingsManager.getInstance();
    }

    private static class Instance {
        private static final DefaultCallStatusHandler CALL_STATUS_HANDLER = new DefaultCallStatusHandler();
    }

    /**
     * Returns the singleton instance of {@link DefaultCallStatusHandler}.
     *
     * @return the singleton instance
     */
    public static DefaultCallStatusHandler getInstance() {
        return Instance.CALL_STATUS_HANDLER;
    }

    /**
     * Processes web-based call status updates (non click-to-call).
     * Handles child call events (INITIATED, IN_PROGRESS, CANCELED, NO_ANSWER, BUSY, FAILED)
     * by looking up the parent call and updating or creating child call records accordingly.
     *
     * @param webhookData the {@link CallStatusWebhookData} containing call event details
     * @param crmResponse the {@link CrmResponse} for error reporting
     */
    @Override
    public void handleWebCall(final CallStatusWebhookData webhookData, final CrmResponse crmResponse) {
        LOGGER.info("Twilio Call Status Webhook Data: callId={}, parentCallId={}, status={}, receiver={}, callType={}, userId={}, duration={}, startTime={}, transferTo={}, transferredBy={}, ringBackEnabled={}",
                webhookData.getCallId(), webhookData.getParentCallId(), webhookData.getCallStatus(), webhookData.getReceiver(), webhookData.getCallType(), webhookData.getUserId(), webhookData.getDuration(), webhookData.getStartTime(), webhookData.getTransferTo(), webhookData.getTransferredBy(), webhookData.isRingBackEnabled());

        final String parentCallId = webhookData.getParentCallId();
        final CallStatus callStatus = webhookData.getCallStatus();

        if (Objects.nonNull(parentCallId)) {
            final String callId = webhookData.getCallId();

            switch (callStatus) {
                case INITIATED -> {
                    final Optional<CallDetails> optionalParentCallDetails = callHistoryManager.getCallDetails(parentCallId);

                    if (optionalParentCallDetails.isPresent()) {
                        final CallDetails callDetails = optionalParentCallDetails.get();

                        callDetails.setId(null);
                        callDetails.setParentCall(false);
                        callDetails.setExternalId(callId);
                        callDetails.setStatus(callStatus);
                        final Collection<ErrorDescriptor> errorDescriptors = callDetailsValidator.validate(List.of(callDetails), Group.Create.class);
                        final Collection<String> violations = errorDescriptors.stream().map(ErrorDescriptor::message).toList();

                        if (violations.isEmpty()) {
                            if (Objects.nonNull(webhookData.getTransferTo())) {
                                callDetails.setForwardDetails(buildCallForwardDetails(callDetails.orgId(), webhookData));
                            }
                            callHistoryManager.save(callDetails);
                        } else {
                            LOGGER.warn("Call History: Create Violations: {}", crmResponse.resolveI18nString(violations));
                            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(NotificationErrorCodes.CALL_HISTORY_CREATE_FAILED)), errorDescriptors));
                        }
                    }
                }
                case IN_PROGRESS -> {
                    final Optional<CallDetails> optionalCallDetails = callHistoryManager.getCallDetails(callId);

                    if (optionalCallDetails.isPresent()) {
                        final CallDetails callDetails = optionalCallDetails.get();

                        voiceCallService.recordCall(callId, callDetails.getUserNumber().getRecordType().isCallRecordingEnabled(webhookData.getCallType()));
                        assignParticipantUser(webhookData, callDetails);
                        callDetails.setStatus(callStatus);
                        final Collection<ErrorDescriptor> errorDescriptors = callDetailsValidator.validate(List.of(callDetails), Group.Update.class);
                        final Collection<String> violations = errorDescriptors.stream().map(ErrorDescriptor::message).toList();

                        if (violations.isEmpty()) {
                            callHistoryManager.update(callDetails);
                        } else {
                            LOGGER.warn("Call History: Update Violations: {}", crmResponse.resolveI18nString(violations));
                            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(NotificationErrorCodes.CALL_HISTORY_UPDATE_FAILED)), errorDescriptors));
                        }
                    }
                }
                case CANCELED, NO_ANSWER, BUSY, FAILED -> {
                    final Optional<CallDetails> optionalCallDetails = callHistoryManager.getCallDetails(callId);

                    if (optionalCallDetails.isPresent()) {
                        final CallDetails callDetails = optionalCallDetails.get();
                        final Long orgId = callDetails.orgId();
                        final String userId = webhookData.getUserId();
                        final Long crmUserId = Objects.isNull(userId) ? userService.getAdminUserId(orgId) : Long.parseLong(userId);

                        secureProcess(orgId,crmUserId, () ->{
                            final VoiceCall voiceCall = voiceCallService.fetchCallDetails(callId);
                            final CallSummary callSummary = new CallSummary();

                            Currency.getCurrencyByCode(voiceCall.getPriceUnit()).ifPresent(currency -> {
                                final Monetary monetary = new Monetary();

                                monetary.setValue(BigDecimal.ZERO);
                                monetary.setCurrency(currency);
                                callSummary.setCost(monetary);
                            });
                            callSummary.setStartTime(new DateTime(voiceCall.getActionTime()));
                            callSummary.setEndTime(new DateTime(voiceCall.getCompletedTime()));
                            callSummary.setDuration(ZERO);
                            callSummary.setBillingDuration(ZERO);
                            final Monetary callCost = callSummary.getCost();

                            if (Objects.nonNull(callCost)) {
                                callCost.setOrgId(orgId);
                            }
                            callSummary.setOrgId(orgId);
                            callDetails.setStatus(callStatus);
                            callDetails.setSummary(callSummary);
                            assignParticipantUser(webhookData, callDetails);
                            final Collection<ErrorDescriptor> errorDescriptors = callDetailsValidator.validate(List.of(callDetails), Group.Update.class);
                            final Collection<String> violations = errorDescriptors.stream().map(ErrorDescriptor::message).toList();

                            if (violations.isEmpty()) {
                                callHistoryManager.update(callDetails);
                            } else {
                                LOGGER.warn("Call History: Update Violations: {}", crmResponse.resolveI18nString(violations));
                                crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(NotificationErrorCodes.CALL_HISTORY_UPDATE_FAILED)), errorDescriptors));
                            }
                        });
                    }
                }
                case COMPLETED -> {
                    final Optional<CallDetails> optionalCallDetails = callHistoryManager.getCallDetails(callId);

                    if (optionalCallDetails.isPresent()) {
                        final CallDetails callDetails = optionalCallDetails.get();
                        final Long orgId = callDetails.orgId();
                        final String userId = webhookData.getUserId();
                        final Long crmUserId = Objects.isNull(userId) ? userService.getAdminUserId(orgId) : Long.parseLong(userId);

                        secureProcess(orgId, crmUserId, () -> {
                            final PhoneSettings phoneSettings = phoneSettingsManager.get(orgId);
                            final VoiceCall voiceCall = voiceCallService.fetchCallDetails(callId);
                            final String voiceCallDuration = voiceCall.getDuration();
                            final String duration = Objects.isNull(voiceCallDuration) ? webhookData.getDuration() : voiceCallDuration;

                            if (Objects.nonNull(duration)) {
                                final DateTime instantTime = new DateTime(Instant.now().toEpochMilli());
                                final Long voiceCallActionTime = voiceCall.getActionTime();
                                final Long startTimeMillis = Objects.isNull(voiceCallActionTime) ? webhookData.getStartTime() : voiceCallActionTime;
                                final CallSummary callSummary = new CallSummary();
                                final BigDecimal price = voiceCall.getPrice();

                                if (Objects.nonNull(price)) {
                                    Currency.getCurrencyByCode(voiceCall.getPriceUnit()).ifPresent(currency -> {
                                        final Monetary monetary = new Monetary();

                                        monetary.setValue(price);
                                        monetary.setCurrency(currency);
                                        monetary.setOrgId(orgId);
                                        callSummary.setCost(monetary);
                                    });
                                }

                                callSummary.setOrgId(orgId);
                                callSummary.setStartTime(Objects.isNull(startTimeMillis) ? instantTime : new DateTime(startTimeMillis));
                                callSummary.setEndTime(instantTime);
                                callSummary.setDuration(duration);
                                callSummary.setBillingDuration(duration);

                                callDetails.setSummary(callSummary);
                            }
                            callDetails.setStatus(callStatus);
                            loadCallRecordingsIntoCallDetails(callDetails, voiceCall);

                            if (phoneSettings.isAutoCreateContactEnabled()) {
                                createContactWithPreferredStage(orgId, crmUserId, callDetails.getContactNumber().getNumberWithCode(), phoneSettings);
                            }
                            callHistoryManager.update(callDetails);
                        });
                    }
                }
                default -> callHistoryManager.updateCallStatus(callId, callStatus);
            }
        }
    }

    /**
     * Loads Twilio call recordings into call details, skipping recordings already saved.
     *
     * @param callDetails the call details to populate
     * @param voiceCall the Twilio voice call with its recordings
     */
    private void loadCallRecordingsIntoCallDetails(final CallDetails callDetails, final VoiceCall voiceCall) {
        final Collection<CallRecord> fetchedRecords = voiceCall.getCallRecords();

        if (Objects.isNull(fetchedRecords) || fetchedRecords.isEmpty()) {
            callDetails.setCallRecordings(null);
            return;
        }
        final Collection<CallRecord> existingRecords = callDetails.getCallRecordings();
        final List<CallRecord> newRecords = fetchedRecords.stream()
                .filter(fetchedRecord -> Objects.isNull(existingRecords) || existingRecords.stream()
                        .noneMatch(existingRecord -> Objects.equals(existingRecord.getUri(), fetchedRecord.getUri())))
                .toList();

        callDetails.setCallRecordings(newRecords.isEmpty() ? null : newRecords);
    }

    /**
     * Handles the click-to-call action based on the provided call status and webhook data.
     *
     * @param webhookData the {@link CallStatusWebhookData} containing call event details
     * @param crmResponse the {@link CrmResponse} for error reporting
     */
    @Override
    public void handleClickToCall(final CallStatusWebhookData webhookData, final CrmResponse crmResponse) {
    }

    /**
     * Builds the {@link CallForwardDetails} from the webhook data.
     *
     * @param orgId       the organization identifier
     * @param webhookData the {@link CallStatusWebhookData} containing transfer information
     * @return the populated {@link CallForwardDetails}
     */
    private CallForwardDetails buildCallForwardDetails(final Long orgId, final CallStatusWebhookData webhookData) {
        final Function<String, Long> longValueParser = inputString -> NumberUtils.parseNumber(inputString).longValue();
        final CallForwardDetails callForwardDetails = new CallForwardDetails();

        callForwardDetails.setForwardedBy(userService.getUser(orgId, longValueParser.apply(webhookData.getTransferredBy())));
        callForwardDetails.setForwardedTo(userService.getUser(orgId, longValueParser.apply(webhookData.getTransferTo())));
        callForwardDetails.setRingBackEnabled(webhookData.isRingBackEnabled());

        return callForwardDetails;
    }

    /**
     * Assigns the participant user to the given {@link CallDetails} for incoming calls.
     *
     * @param webhookData the {@link CallStatusWebhookData} containing participant information
     * @param callDetails the {@link CallDetails} to assign the participant to
     */
    private void assignParticipantUser(final CallStatusWebhookData webhookData, final CallDetails callDetails) {
        if (CallType.INCOMING == webhookData.getCallType()) {
            final Long orgId = callDetails.orgId();
            final String transferTo = webhookData.getTransferTo();
            final long participantUserId;

            if (Objects.isNull(transferTo)) {
                final String recipient = webhookData.getReceiver();

                participantUserId = NumberUtils.parseNumber(recipient.substring(recipient.lastIndexOf(UNDERSCORE) + ONE)).longValue();
            } else {
                participantUserId = NumberUtils.parseNumber(transferTo).longValue();

                callDetails.setForwardDetails(buildCallForwardDetails(orgId, webhookData));
            }
            callDetails.setParticipantUser(userService.getUser(orgId, participantUserId));
        }
    }
}

