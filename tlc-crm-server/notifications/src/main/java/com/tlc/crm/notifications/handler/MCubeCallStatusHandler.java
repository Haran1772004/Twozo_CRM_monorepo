package com.tlc.crm.notifications.handler;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.StringUtil;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.notifications.api.call.manager.CallHistoryManager;
import com.tlc.crm.notifications.api.call.manager.PhoneNumberManager;
import com.tlc.crm.notifications.api.call.manager.PhoneSettingsManager;
import com.tlc.crm.notifications.api.call.model.CallDetails;
import com.tlc.crm.notifications.api.call.model.CallSummary;
import com.tlc.crm.notifications.api.call.model.PhoneNumber;
import com.tlc.crm.notifications.api.call.model.PhoneSettings;
import com.tlc.crm.notifications.api.call.model.RecordType;
import com.tlc.crm.notifications.internal.status.NotificationErrorCodes;
import com.tlc.crm.notifications.marketplace.api.manager.MCubeActionManager;
import com.tlc.crm.notifications.marketplace.api.model.CrmMCubeUser;
import com.tlc.crm.notifications.validator.CallDetailsValidator;
import com.tlc.iam.common.models.User;
import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.model.number.PhoneNumberUtil;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.notification.model.call.CallRecord;
import com.tlc.notification.model.call.CallStatus;
import com.tlc.notification.model.call.CallType;
import com.tlc.notification.model.telephone.TelephoneProvider;
import com.tlc.notification.model.telephone.mcube.MCubeAuthData;
import com.tlc.notification.service.telephony.mcube.MCubeService;
import com.tlc.notification.webhook.model.CallStatusWebhookData;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.type.Group;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles all call status webhook processing, including both web-based calls
 * and click-to-call scenarios. Manages call lifecycle events, participant assignment,
 * call forwarding, and recording retrieval.
 *
 * @author Sathish
 * @version 1.0
 */
public class MCubeCallStatusHandler extends AbstractCallStatusHandler implements CallStatusHandler, SecureUserTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MCubeCallStatusHandler.class);
    private static final String PLUS = "+";
    private static final String ZERO = "0";

    private final CallHistoryManager callHistoryManager;
    private final OrganizationService organizationService;
    private final UserService userService;
    private final com.tlc.iam.client.UserService iamUserService;
    private final CallDetailsValidator callDetailsValidator;
    private final PhoneNumberManager phoneNumberManager;
    private final ContactManager contactManager;
    private final PhoneManager phoneManager;
    private final DealManager dealManager;
    private final PhoneSettingsManager phoneSettingsManager;
    private final MCubeActionManager mCubeActionManager;
    private final MCubeService mCubeService;

    private MCubeCallStatusHandler() {
        this.callHistoryManager = CallHistoryManager.getInstance();
        this.organizationService = OrganizationService.getInstance();
        this.userService = UserService.getInstance();
        this.iamUserService = com.tlc.iam.client.UserService.getInstance();
        this.callDetailsValidator = CallDetailsValidator.getInstance();
        this.phoneNumberManager = PhoneNumberManager.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.phoneManager = PhoneManager.getInstance();
        this.dealManager = DealManager.getInstance();
        this.phoneSettingsManager = PhoneSettingsManager.getInstance();
        this.mCubeActionManager = MCubeActionManager.getInstance();
        this.mCubeService = MCubeService.getInstance();
    }

    private static class Instance {
        private static final MCubeCallStatusHandler CALL_STATUS_HANDLER = new MCubeCallStatusHandler();
    }

    /**
     * Returns the singleton instance of {@link MCubeCallStatusHandler}.
     *
     * @return the singleton instance
     */
    public static MCubeCallStatusHandler getInstance() {
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
    }

    /**
     * Handles the click-to-call action based on the provided call status and webhook data.
     *
     * @param webhookData the {@link CallStatusWebhookData} containing call event details
     * @param crmResponse the {@link CrmResponse} for error reporting
     */
    @Override
    public void handleClickToCall(final CallStatusWebhookData webhookData, final CrmResponse crmResponse) {
        final CallDetails callDetails = new CallDetails();
        final CallType callType = webhookData.getCallType();
        final CallStatus callStatus = webhookData.getCallStatus();
        final String callerNumber = webhookData.getCaller();
        final String callId = webhookData.getCallId();
        final String receiverNumber = webhookData.getReceiver();
        final String routingNumber = webhookData.getRoutingNumber();
        final String userNumber;
        final String customerNumber;

        switch (callType) {
            case INCOMING -> {
                userNumber = receiverNumber;
                customerNumber = addCountryCodePrefix(callerNumber);
            }
            default -> {
                userNumber = callerNumber;
                customerNumber = addCountryCodePrefix(receiverNumber);
            }
        }

        callDetails.setExternalId(callId);
        callDetails.setType(callType);
        callDetails.setStatus(callStatus);
        LOGGER.info("PHONE: Call Type:{} | Call Status: {} | Webhookdata: Caller {}, Receiver: {}, Transfered BY: {}, Transfered To: {}, CallId:{}, ParentCallID: {}, RecordCallName: {}, Extra params:{}, UserId: {}",callType, callStatus, webhookData.getCaller(), webhookData.getReceiver(), webhookData.getTransferredBy(), webhookData.getTransferTo(), webhookData.getCallId(), webhookData.getParentCallId(), webhookData.getRecordFileName(), webhookData.getExtraParams(), webhookData.getUserId());
        LOGGER.info("Click-to-Call: Processing call status - {} for provider - {}. Webhook Data - {}", callStatus, TelephoneProvider.M_CUBE, webhookData);

        switch (callStatus) {
            case COMPLETED, BUSY, CANCELED, NO_ANSWER -> {
                iamUserService.fetchUsersByPhones(List.of(userNumber, String.format("+91%s", userNumber)), new EventCallback<>() {

                    @Override
                    public void onSuccess(final Collection<User> users) {
                        try {

                            for (final User user : users) {
                                LOGGER.info("Mcube: User orgid: {}, userid: {}, phone number: {}, total users: {}",user.orgId(), user.id(), user.getMobile(), users.size());
                                final Long orgId = organizationService.getOrgId(user.orgId());
                                final Optional<CrmUser> optionalCrmUser = userService.getUserByIamId(orgId, user.id());

                                if (optionalCrmUser.isPresent()) {
                                    final CrmUser crmUser = optionalCrmUser.get();
                                    final Long userId = crmUser.id();

                                    secureProcess(crmUser, () -> {
                                        final Optional<CrmMCubeUser> crmMCubeUser = optionalCrmUser
                                                .flatMap(duplicateCrmUser -> mCubeActionManager.getUser(orgId, duplicateCrmUser.id()))
                                                .filter(mCubeUser -> Objects.nonNull(mCubeUser.getMCubeAuthData().getAuthToken())
                                                        && Objects.equals(mCubeUser.getSyncStatus(), CrmMCubeUser.SyncStatus.CONNECTED));

                                        if (crmMCubeUser.isEmpty()) {
                                            LOGGER.info("Mcube: mcube user is empty");
                                            return;
                                        }

                                        if (Objects.equals(CallStatus.COMPLETED, callStatus)) {
                                            loadCallRecordingIntoCallDetails(orgId, callDetails, webhookData);
                                        }

                                        final Collection<Contact> contacts = contactManager.getContactsByPhone(orgId, "+91" + customerNumber.substring(1));
                                        final Optional<Contact> optionalContact = contacts.stream().findFirst();

                                        callDetails.setOrgId(orgId);
                                        callDetails.setParticipantUser(userService.getUser(orgId, userId));
                                        phoneNumberManager.getNumber(orgId, routingNumber).ifPresentOrElse(callDetails::setUserNumber,
                                                () -> {
                                                    final PhoneNumber phoneNumber = new PhoneNumber();

                                                    phoneNumber.setActive(true);
                                                    phoneNumber.setNumber(routingNumber);
                                                    phoneNumber.setOrgId(orgId);
                                                    phoneNumber.setTelephoneProvider(TelephoneProvider.M_CUBE);
                                                    phoneNumber.setRecordType(RecordType.DO_NOT_RECORD_CALLS);
                                                    callDetails.setUserNumber(phoneNumber);
                                                });

                                        if (optionalContact.isPresent()) {
                                            final Contact contact = optionalContact.get();

                                            dealManager.findSingleDealByContact(orgId, contact.id()).ifPresent(callDetails::setDeal);
                                            callDetails.setContact(contact);
                                            callDetails.setContactNumber(contact.getPrimaryPhone());
                                        } else {
                                            final PhoneSettings phoneSettings = phoneSettingsManager.get(orgId);

                                            if (phoneSettings.isAutoCreateContactEnabled()) {
                                                final Contact contact = createContactWithPreferredStage(orgId, userId, "+91" + customerNumber.substring(1), phoneSettings);

                                                if (Objects.nonNull(contact.id())) {
                                                    callDetails.setContact(contact);
                                                    callDetails.setContactNumber(contact.getPrimaryPhone());
                                                }
                                            }

                                            if (Objects.isNull(callDetails.getContactNumber())) {
                                                callDetails.setContactNumber(createPhone(orgId, "+91" + customerNumber.substring(1)));
                                            }
                                        }

                                        final long currentTime = Instant.now().toEpochMilli();
                                        final String duration = webhookData.getDuration();
                                        final Long startTime = webhookData.getStartTime();
                                        final DateTime instantTime = new DateTime(Instant.now().toEpochMilli());

                                        if (Objects.nonNull(duration)) {
                                            final CallSummary callSummary = new CallSummary();

                                            switch (callStatus) {
                                                case BUSY, CANCELED, NO_ANSWER -> {
                                                    callSummary.setOrgId(orgId);
                                                    callSummary.setStartTime(new DateTime(currentTime));
                                                    callSummary.setEndTime(new DateTime(currentTime));
                                                    callSummary.setDuration(ZERO);
                                                    callSummary.setBillingDuration(ZERO);
                                                }
                                                default -> {
                                                    callSummary.setOrgId(orgId);
                                                    callSummary.setStartTime(Objects.isNull(startTime) ? instantTime : new DateTime(webhookData.getStartTime()));
                                                    callSummary.setEndTime(instantTime);
                                                    callSummary.setDuration(duration);
                                                    callSummary.setBillingDuration(ZERO);
                                                }
                                            }

                                            callDetails.setSummary(callSummary);
                                        }

                                        final Collection<ErrorDescriptor> errorDescriptors = callDetailsValidator.validate(List.of(callDetails), Group.Create.class);
                                        final Collection<String> violations = errorDescriptors.stream().map(ErrorDescriptor::message).toList();

                                        if (violations.isEmpty()) {
                                            callHistoryManager.save(callDetails);
                                            LOGGER.info("Call History: Create Success: Provide - {}, CallId - {} and UserId - {}", TelephoneProvider.M_CUBE, callId, userId);
                                        } else {
                                            LOGGER.warn("Call History: Create Violations: {}", crmResponse.resolveI18nString(violations));
                                            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(NotificationErrorCodes.CALL_HISTORY_CREATE_FAILED)), errorDescriptors));
                                        }
                                    });
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("Error processing click-to-call completion for provider - {} callId - {} error - {}", TelephoneProvider.M_CUBE, callId, e.getMessage(), e);
                            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(NotificationErrorCodes.CALL_HISTORY_CREATE_FAILED), "i18n.notifications.call.history.create.failed")));
                        } finally {
                            crmResponse.send();
                        }
                    }

                    @Override
                    public void onFailure(final Throwable cause) {
                        LOGGER.error("Error fetching user by phone for click-to-call completion for provider - {} callId - {} error - {}", TelephoneProvider.M_CUBE, callId, cause.getMessage(), cause);
                        crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(NotificationErrorCodes.CALL_HISTORY_CREATE_FAILED), "i18n.notifications.call.history.create.failed")));
                        crmResponse.send();
                    }
                });
            }
            default -> callHistoryManager.updateCallStatus(callId, callStatus);
        }
    }

    /**
     * Loads call recording details into the call history based on webhook data.
     *
     * @param orgId                  Represents the org id
     * @param callDetails            Represents the call details to populate
     * @param callStatusWebhookData  Represents the {@link CallStatusWebhookData} containing call event details
     */
    private void loadCallRecordingIntoCallDetails(final Long orgId, final CallDetails callDetails, final CallStatusWebhookData callStatusWebhookData) {
        final String recordFileName = callStatusWebhookData.getRecordFileName();

        if (Objects.isNull(recordFileName)) {
            return;
        }
        final String callId = callStatusWebhookData.getCallId();
        final Optional<MCubeAuthData> authData = mCubeActionManager.getAuthData(orgId, null);

        authData.ifPresent(mCubeAuthData -> {
            final String recordedURL = mCubeService.buildVoiceRecordUrl(mCubeAuthData, callId);
            final Collection<CallRecord> existingRecords = callDetails.getCallRecordings();
            final boolean isAlreadyExists = Objects.nonNull(existingRecords) && existingRecords.stream().anyMatch(callRecord -> Objects.equals(callRecord.getUri(), recordedURL));

            if (isAlreadyExists) {
                callDetails.setCallRecordings(null);
                return;
            }
            final CallRecord callRecord = new CallRecord();

            callRecord.setUri(recordedURL);
            callDetails.setCallRecordings(List.of(callRecord));
        });
    }

    /**
     * Creates a {@link Phone} for the given organization, receiver number, and optional contact number ID.
     *
     * @param orgId          the organization identifier
     * @param customerNumber the customer phone number
     * @return the constructed {@link Phone} instance
     */
    private Phone createPhone(final Long orgId, final String customerNumber) {
        final Phone phone = new Phone();
        final com.tlc.l10n.model.number.PhoneNumber number = PhoneNumberUtil.parse(customerNumber);

        phone.setOrgId(orgId);
        phone.setNumber(customerNumber);
        phone.setType(Phone.Type.MOBILE);
        phone.setCountry(Country.INDIA);
        phoneManager.create(phone);
        return phone;
    }

    /**
     * Ensures that the given phone number includes a country code prefix ('+').
     *
     * @param number the phone number to validate or modify
     * @return the phone number with a '+' country code prefix
     */
    private String addCountryCodePrefix(final String number) {
        return number.startsWith(PLUS) ? number : String.join(StringUtil.EMPTY_STRING, PLUS, number);
    }
}

