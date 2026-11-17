package com.tlc.crm.notifications.handler;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheConfig;
import com.tlc.cache.CacheManager;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.StringUtil;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.notifications.api.call.manager.CallHistoryManager;
import com.tlc.crm.notifications.api.call.manager.PhoneSettingsManager;
import com.tlc.crm.notifications.api.call.model.CallDetails;
import com.tlc.crm.notifications.api.call.model.CallSummary;
import com.tlc.crm.notifications.api.call.model.PhoneNumber;
import com.tlc.crm.notifications.api.call.model.PhoneSettings;
import com.tlc.crm.notifications.api.call.model.RecordType;
import com.tlc.crm.notifications.fields.NotificationFields;
import com.tlc.crm.notifications.internal.status.NotificationErrorCodes;
import com.tlc.crm.notifications.marketplace.api.manager.TeleCMIActionManager;
import com.tlc.crm.notifications.marketplace.api.model.CrmTeleCMIUser;
import com.tlc.crm.notifications.validator.CallDetailsValidator;
import com.tlc.iam.common.models.User;
import com.tlc.l10n.model.number.PhoneNumberUtil;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.notification.model.call.CallRecord;
import com.tlc.notification.model.call.CallStatus;
import com.tlc.notification.model.call.CallType;
import com.tlc.notification.model.telephone.TelephoneProvider;
import com.tlc.notification.model.telephone.telecmi.TeleCMIAuthData;
import com.tlc.notification.service.telephony.telecmi.TeleCMIService;
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
public class TeleCMICallStatusHandler extends AbstractCallStatusHandler implements CallStatusHandler, SecureUserTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeleCMICallStatusHandler.class);
    private static final String ZERO = "0";
    private static final String PLUS = "+";

    private final CallHistoryManager callHistoryManager;
    private final CallDetailsValidator callDetailsValidator;
    private final PhoneManager phoneManager;
    private final DealManager dealManager;
    private final TeleCMIActionManager teleCMIActionManager;
    private final TeleCMIService teleCMIService;
    private final PhoneSettingsManager phoneSettingsManager;
    private final com.tlc.iam.client.UserService iamUserService;
    private final OrganizationService organizationService;
    private final Cache<String, String> receiverCache;

    private TeleCMICallStatusHandler() {
        this.callHistoryManager = CallHistoryManager.getInstance();
        this.callDetailsValidator = CallDetailsValidator.getInstance();
        this.phoneManager = PhoneManager.getInstance();
        this.dealManager = DealManager.getInstance();
        this.teleCMIActionManager = TeleCMIActionManager.getInstance();
        this.teleCMIService = TeleCMIService.getInstance();
        this.phoneSettingsManager = PhoneSettingsManager.getInstance();
        this.iamUserService = com.tlc.iam.client.UserService.getInstance();
        this.organizationService = OrganizationService.getInstance();
        this.receiverCache = CacheManager.getInstance().createCache(new CacheConfig<String, String>().setExpireAfterCreate(120));
    }

    private static class Instance {
        private static final TeleCMICallStatusHandler CALL_STATUS_HANDLER = new TeleCMICallStatusHandler();
    }

    /**
     * Returns the singleton instance of {@link TeleCMICallStatusHandler}.
     *
     * @return the singleton instance
     */
    public static TeleCMICallStatusHandler getInstance() {
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
        final CallType callType = webhookData.getCallType();
        final CallStatus callStatus = webhookData.getCallStatus();
        final String receiver = webhookData.getReceiver();
        final String callId = webhookData.getCallId();
        final String caller = webhookData.getCaller();
        final String userId = webhookData.getUserId();
        LOGGER.info("PHONE: Call Type:{} | Call Status: {} | Webhookdata: Caller {}, Receiver: {}, Transfered BY: {}, Transfered To: {}, CallId:{}, ParentCallID: {}, RecordCallName: {}, Extra params:{}, UserId: {}",callType, callStatus, webhookData.getCaller(), webhookData.getReceiver(), webhookData.getTransferredBy(), webhookData.getTransferTo(), webhookData.getCallId(), webhookData.getParentCallId(), webhookData.getRecordFileName(), webhookData.getExtraParams(), webhookData.getUserId());

        switch (callStatus) {
            case WAITING -> {
                switch (callType) {
                    case INCOMING -> {
                        if (Objects.nonNull(receiver)) {
                            receiverCache.put(callId, receiver);
                        }
                    }
                }
            }
            case INITIATED -> {

                switch (callType) {
                    case OUTGOING -> {
                        if (Objects.nonNull(caller) && Objects.nonNull(receiver) && Objects.nonNull(userId)) {
                            final Optional<CrmTeleCMIUser> optionalCrmTeleCMIUser = teleCMIActionManager.findUser(userId);

                            if (optionalCrmTeleCMIUser.isEmpty()) {
                                return;
                            }
                            final CrmUser crmUser = optionalCrmTeleCMIUser.get().getCrmUser();
                            final Long orgId = crmUser.orgId();

                            final EventCallback<User> userFetchCallback = new EventCallback<>() {

                                @Override
                                public void onSuccess(final User user) {
                                    final String userMobile = user.getMobile();

                                    if (!Objects.equals(PhoneNumberUtil.parse(userMobile).getNumber(), PhoneNumberUtil.parse(addCountryCodePrefix(receiver)).getNumber())) {
                                        final JsonObject customParams = webhookData.getExtraParams();
                                        final boolean hasNoCustomParams = Objects.isNull(customParams);
                                        final String contactNumberId = hasNoCustomParams ? null : customParams.getNullableValue(NotificationFields.CONTACT_NUMBER_ID.getName());
                                        final String contactId = hasNoCustomParams ? null : customParams.getNullableValue(NotificationFields.CONTACT_ID.getName());
                                        final String dealId = hasNoCustomParams ? null : customParams.getNullableValue(NotificationFields.DEAL_ID.getName());

                                        secureProcess(orgId, crmUser.id(), () -> {
                                            final PhoneNumber phoneNumber = new PhoneNumber();
                                            final Phone phone = buildPhone(orgId, receiver, contactNumberId);
                                            final CallDetails callDetails = new CallDetails();

                                            phoneNumber.setOrgId(orgId);
                                            phoneNumber.setRecordType(RecordType.RECORD_ALL_CALLS);
                                            phoneNumber.setNumber(addCountryCodePrefix(caller));
                                            phoneNumber.setActive(true);
                                            phoneNumber.setTelephoneProvider(TelephoneProvider.TELE_CMI);

                                            callDetails.setOrgId(orgId);
                                            callDetails.setExternalId(callId);
                                            callDetails.setType(callType);
                                            callDetails.setUserNumber(phoneNumber);
                                            callDetails.setParticipantUser(crmUser);

                                            if (Objects.nonNull(contactId)) {
                                                final long contactIdValue = Long.parseLong(contactId);
                                                final Contact contact = new Contact();

                                                contact.setId(contactIdValue);
                                                contact.setOrgId(orgId);
                                                callDetails.setContact(contact);

                                                dealManager.findSingleDealByContact(orgId, contactIdValue)
                                                        .ifPresent(callDetails::setDeal);
                                            }

                                            if (Objects.nonNull(dealId)) {
                                                final Deal deal = new Deal();

                                                deal.setId(Long.parseLong(dealId));
                                                callDetails.setDeal(deal);
                                            }
                                            callDetails.setContactNumber(phone);
                                            callDetails.setStatus(callStatus);
                                            final Collection<ErrorDescriptor> errorDescriptors = callDetailsValidator.validate(List.of(callDetails), Group.Create.class);
                                            final Collection<String> violations = errorDescriptors.stream().map(ErrorDescriptor::message).toList();

                                            if (violations.isEmpty()) {
                                                callHistoryManager.save(callDetails);
                                            } else {
                                                LOGGER.warn("Call History: Create Violations: {}", crmResponse.resolveI18nString(violations));
                                                crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(NotificationErrorCodes.CALL_HISTORY_CREATE_FAILED)), errorDescriptors));
                                            }
                                        });
                                    }
                                }

                                @Override
                                public void onFailure(final Throwable throwable) {
                                    LOGGER.error("Phone: Failed to fetch user data. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                                }
                            };

                            iamUserService.fetchUser(organizationService.getIamOrgId(orgId), crmUser.getIamId(), userFetchCallback);
                        }
                    }
                }
            }
            case COMPLETED -> {

                switch (callType) {
                    case OUTGOING -> {
                        if (Objects.nonNull(caller) && Objects.nonNull(receiver) && Objects.nonNull(userId)) {
                            final Optional<CallDetails> optionalCallDetails = callHistoryManager.getCallDetails(callId);

                            optionalCallDetails.ifPresent(callDetails -> {
                                if (CallStatus.NO_ANSWER != callDetails.getStatus()) {
                                    final Long orgId = callDetails.orgId();
                                    final Optional<CrmTeleCMIUser> optionalCrmTeleCMIUser = teleCMIActionManager.findUser(userId);

                                    if (optionalCrmTeleCMIUser.isEmpty()) {
                                        return;
                                    }
                                    final CrmUser crmUser = optionalCrmTeleCMIUser.get().getCrmUser();

                                    secureProcess(orgId, crmUser.id(), () -> {
                                        final Contact contact = callDetails.getContact();
                                        final String duration = webhookData.getDuration();
                                        final Long startTime = webhookData.getStartTime();
                                        final long currentTime = Instant.now().toEpochMilli();
                                        final CallSummary callSummary = new CallSummary();

                                        callSummary.setOrgId(orgId);
                                        callSummary.setStartTime(new DateTime(startTime));
                                        callSummary.setEndTime(new DateTime(currentTime));

                                        if (Objects.isNull(duration)) {
                                            callSummary.setDuration(String.valueOf((currentTime - startTime) / 1000));
                                        } else {
                                            callSummary.setDuration(duration);
                                        }
                                        callSummary.setBillingDuration(ZERO);

                                        callDetails.setSummary(callSummary);

                                        final EventCallback<User> userFetchCallback = new EventCallback<>() {

                                            @Override
                                            public void onSuccess(final User user) {
                                                try {
                                                    if (Objects.nonNull(contact)) {
                                                        final JsonObject customParams = webhookData.getExtraParams();
                                                        final String contactId = Objects.isNull(customParams) ? null : customParams.getNullableValue(NotificationFields.CONTACT_ID.getName());

                                                        if (!Objects.equals(contactId, contact.id().toString())) {
                                                            callDetails.setContact(null);
                                                        }
                                                    } else {
                                                        final PhoneSettings phoneSettings = phoneSettingsManager.get(orgId);
                                                        final String userMobile = user.getMobile();
                                                        final Long crmUserId = crmUser.id();

                                                        if (!Objects.equals(PhoneNumberUtil.parse(userMobile).getNumber(), PhoneNumberUtil.parse(addCountryCodePrefix(receiver)).getNumber())
                                                                && phoneSettings.isAutoCreateContactEnabled()) {
                                                            final Contact createdContact = createContactWithPreferredStage(orgId, crmUserId, addCountryCodePrefix(receiver), phoneSettings);

                                                            if (Objects.nonNull(createdContact.id())) {
                                                                callDetails.setContact(createdContact);
                                                            }
                                                        }
                                                    }

                                                    loadCallRecordingIntoCallDetails(orgId, callDetails, webhookData);
                                                    callDetails.setStatus(callStatus);
                                                    final Collection<ErrorDescriptor> errorDescriptors = callDetailsValidator.validate(List.of(callDetails), Group.Update.class);
                                                    final Collection<String> violations = errorDescriptors.stream().map(ErrorDescriptor::message).toList();

                                                    if (violations.isEmpty()) {
                                                        callHistoryManager.update(callDetails);
                                                    } else {
                                                        LOGGER.warn("Call History: Update Violations: {}", crmResponse.resolveI18nString(violations));
                                                        crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(NotificationErrorCodes.CALL_HISTORY_UPDATE_FAILED)), errorDescriptors));
                                                    }
                                                } catch (final Exception exception) {
                                                    LOGGER.error("Call History: Failed to update call details in Completed, Call ID : {}, Org ID : {}", callId, orgId, exception);
                                                    crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(NotificationErrorCodes.CALL_HISTORY_UPDATE_FAILED))));
                                                }
                                            }

                                            @Override
                                            public void onFailure(final Throwable throwable) {
                                                LOGGER.error("Phone: Failed to fetch user data. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                                            }
                                        };

                                        iamUserService.fetchUser(organizationService.getIamOrgId(orgId), crmUser.getIamId(), userFetchCallback);
                                    });
                                }
                            });
                        }

                        final boolean isCallHandled = Objects.nonNull(caller) && Objects.nonNull(receiver) && Objects.nonNull(userId);
                        final Optional<CallDetails> optionalCallDetails = isCallHandled ? Optional.empty() : callHistoryManager.getCallDetails(callId);

                        optionalCallDetails.ifPresent(callDetails -> {
                            if (CallStatus.NO_ANSWER != callDetails.getStatus()) {

                                secureProcess(callDetails.getParticipantUser(), () -> {
                                    final Long orgId = callDetails.orgId();
                                    final String duration = webhookData.getDuration();
                                    final Long startTime = webhookData.getStartTime();
                                    final long currentTime = Instant.now().toEpochMilli();
                                    final CallSummary existingCallSummary = callDetails.getSummary();

                                    loadCallRecordingIntoCallDetails(orgId, callDetails, webhookData);

                                    if (Objects.isNull(existingCallSummary)) {
                                        final CallSummary callSummary = new CallSummary();

                                        callSummary.setOrgId(orgId);
                                        callSummary.setStartTime(Objects.isNull(startTime) ? new DateTime(currentTime) : new DateTime(startTime));
                                        callSummary.setEndTime(new DateTime(currentTime));

                                        if (Objects.isNull(duration)) {
                                            callSummary.setDuration(String.valueOf((currentTime - startTime) / 1000));
                                        } else {
                                            callSummary.setDuration(duration);
                                        }
                                        callSummary.setBillingDuration(ZERO);

                                        callDetails.setSummary(callSummary);
                                    } else if (Objects.nonNull(duration)) {
                                        existingCallSummary.setDuration(duration);
                                        existingCallSummary.setEndTime(new DateTime(currentTime));
                                    }
                                    callHistoryManager.update(callDetails);
                                });
                            }
                        });
                    }

                    case INCOMING -> {
                        if (Objects.nonNull(caller) && Objects.nonNull(userId)) {
                            final Optional<CrmTeleCMIUser> optionalCrmTeleCMIUser = teleCMIActionManager.findUser(userId);

                            if (optionalCrmTeleCMIUser.isEmpty()) {
                                return;
                            }
                            final CrmUser crmUser = optionalCrmTeleCMIUser.get().getCrmUser();
                            final Long orgId = crmUser.orgId();

                            secureProcess(orgId, crmUser.id(), () -> {
                                final Optional<CallDetails> existingCallDetails = callHistoryManager.getCallDetails(callId);

                                if (existingCallDetails.isPresent()) {
                                    updateIncomingCallAsAnswered(existingCallDetails.get(), crmUser, webhookData, crmResponse);
                                    return;
                                }
                                final PhoneSettings phoneSettings = phoneSettingsManager.get(orgId);
                                final Phone phone = buildPhone(orgId, caller, null);
                                final CallDetails callDetails = new CallDetails();
                                final String duration = webhookData.getDuration();
                                final Long startTime = webhookData.getStartTime();
                                final long currentTime = Instant.now().toEpochMilli();
                                final CallSummary callSummary = new CallSummary();

                                callSummary.setOrgId(orgId);
                                callSummary.setStartTime(new DateTime(startTime));
                                callSummary.setEndTime(new DateTime(currentTime));

                                if (Objects.isNull(duration)) {
                                    callSummary.setDuration(String.valueOf((currentTime - startTime) / 1000));
                                } else {
                                    callSummary.setDuration(duration);
                                }
                                LOGGER.info("Telecmi - Duration:{}", callSummary.getDuration());
                                callSummary.setBillingDuration(ZERO);

                                callDetails.setSummary(callSummary);

                                if (receiverCache.containsKey(callId)) {
                                    final PhoneNumber phoneNumber = new PhoneNumber();

                                    phoneNumber.setOrgId(orgId);
                                    phoneNumber.setRecordType(RecordType.RECORD_ALL_CALLS);
                                    phoneNumber.setNumber(addCountryCodePrefix(receiverCache.get(callId)));
                                    phoneNumber.setActive(true);
                                    phoneNumber.setTelephoneProvider(TelephoneProvider.TELE_CMI);

                                    callDetails.setUserNumber(phoneNumber);
                                    receiverCache.remove(callId);
                                }
                                callDetails.setOrgId(orgId);
                                callDetails.setExternalId(callId);
                                callDetails.setType(callType);
                                callDetails.setParticipantUser(crmUser);
                                callDetails.setContactNumber(phone);
                                callDetails.setStatus(callStatus);
                                loadCallRecordingIntoCallDetails(orgId, callDetails, webhookData);

                                final String number = PhoneNumberUtil.parse(phone.getNumberWithCode()).getNumberWithCountryCode();
                                final Collection<Contact> existingContacts = contactManager.getContactsByPhone(orgId, number);

                                if (!existingContacts.isEmpty()) {
                                    setExistingEntities(orgId, callDetails, existingContacts);
                                } else if (phoneSettings.isAutoCreateContactEnabled()) {
                                    final Contact contact = createContactWithPreferredStage(orgId, crmUser.id(), addCountryCodePrefix(caller), phoneSettings);

                                    if (Objects.nonNull(contact.id())) {
                                        callDetails.setContact(contact);
                                    }
                                } else {
                                    callDetails.setContact(null);
                                }

                                final Collection<ErrorDescriptor> errorDescriptors = callDetailsValidator.validate(List.of(callDetails), Group.Create.class);
                                final Collection<String> violations = errorDescriptors.stream().map(ErrorDescriptor::message).toList();

                                if (violations.isEmpty()) {
                                    callHistoryManager.save(callDetails);
                                } else {
                                    LOGGER.warn("Call History: Create Violations: {}", crmResponse.resolveI18nString(violations));
                                    crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(NotificationErrorCodes.CALL_HISTORY_CREATE_FAILED)), errorDescriptors));
                                }
                            });
                        }
                    }
                }
            }
            case NO_ANSWER -> {
                switch (callType) {
                    case OUTGOING -> callHistoryManager.getCallDetails(callId).ifPresent(callDetails -> {
                        final CrmUser crmUser = callDetails.getParticipantUser();

                        secureProcess(crmUser, () -> {
                            final CallSummary callSummary = new CallSummary();
                            final Contact contact = callDetails.getContact();
                            final Long orgId = callDetails.orgId();
                            final long currentTime = Instant.now().toEpochMilli();
                            final String contactNumber = callDetails.getContactNumber().getNumber();

                            callSummary.setOrgId(orgId);
                            callSummary.setStartTime(new DateTime(currentTime));
                            callSummary.setEndTime(new DateTime(currentTime));
                            callSummary.setDuration(ZERO);
                            callSummary.setBillingDuration(ZERO);

                            callDetails.setSummary(callSummary);
                            callDetails.setStatus(callStatus);

                            crmResponse.convertIntoAsync();
                            final EventCallback<User> userFetchCallback = new EventCallback<>() {

                                @Override
                                public void onSuccess(final User user) {
                                    try {

                                        if (Objects.nonNull(contact)) {
                                            final JsonObject customParams = webhookData.getExtraParams();
                                            final String contactId = Objects.isNull(customParams) ? null : customParams.getNullableValue(NotificationFields.CONTACT_ID.getName());

                                            if (!Objects.equals(contactId, contact.id().toString())) {
                                                callDetails.setContact(null);
                                            }
                                        } else if (Objects.nonNull(receiver) || Objects.nonNull(contactNumber)) {
                                            final String number = Objects.isNull(receiver) ? contactNumber : receiver;
                                            final PhoneSettings phoneSettings = phoneSettingsManager.get(orgId);
                                            final String userMobile = user.getMobile();
                                            final Long crmUserId = crmUser.id();

                                            if (!Objects.equals(PhoneNumberUtil.parse(userMobile).getNumber(), PhoneNumberUtil.parse(addCountryCodePrefix(number)).getNumber())
                                                    && phoneSettings.isAutoCreateContactEnabled()) {
                                                final Contact createdContact = createContactWithPreferredStage(orgId, crmUserId, addCountryCodePrefix(number), phoneSettings);

                                                if (Objects.nonNull(createdContact.id())) {
                                                    callDetails.setContact(createdContact);
                                                }
                                            }
                                        }
                                        final Collection<ErrorDescriptor> errorDescriptors = callDetailsValidator.validate(List.of(callDetails), Group.Update.class);
                                        final Collection<String> violations = errorDescriptors.stream().map(ErrorDescriptor::message).toList();

                                        if (violations.isEmpty()) {
                                            callHistoryManager.update(callDetails);
                                        } else {
                                            LOGGER.warn("Call History: Update Violations: {}", crmResponse.resolveI18nString(violations));
                                            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(NotificationErrorCodes.CALL_HISTORY_UPDATE_FAILED)), errorDescriptors));
                                        }
                                    } catch (final Exception exception) {
                                        LOGGER.error("Call History: Failed to update call details in No_Answer", exception);
                                        crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(NotificationErrorCodes.CALL_HISTORY_UPDATE_FAILED))));
                                    } finally {
                                        crmResponse.send();
                                    }
                                }

                                @Override
                                public void onFailure(final Throwable throwable) {
                                    LOGGER.error("Phone: Failed to fetch user data. Reason: {}, Org Id: {}", throwable.getMessage(), orgId);
                                }
                            };

                            iamUserService.fetchUser(organizationService.getIamOrgId(orgId), crmUser.getIamId(), userFetchCallback);
                        });
                    });
                    case INCOMING -> {
                        if (Objects.nonNull(caller) && Objects.nonNull(userId)) {
                            final Optional<CrmTeleCMIUser> optionalCrmTeleCMIUser = teleCMIActionManager.findUser(userId);

                            if (optionalCrmTeleCMIUser.isEmpty()) {
                                return;
                            }
                            final CrmUser crmUser = optionalCrmTeleCMIUser.get().getCrmUser();
                            final Long orgId = crmUser.orgId();

                            if (callHistoryManager.getCallDetails(callId).isPresent()) {
                                return;
                            }

                            secureProcess(orgId, crmUser.id(), () -> {
                                final PhoneSettings phoneSettings = phoneSettingsManager.get(orgId);
                                final Phone phone = buildPhone(orgId, caller, null);
                                final CallDetails callDetails = new CallDetails();
                                final CallSummary callSummary = new CallSummary();
                                final long currentTime = Instant.now().toEpochMilli();

                                if (receiverCache.containsKey(callId)) {
                                    final PhoneNumber phoneNumber = new PhoneNumber();

                                    phoneNumber.setOrgId(orgId);
                                    phoneNumber.setRecordType(RecordType.RECORD_ALL_CALLS);
                                    phoneNumber.setNumber(addCountryCodePrefix(receiverCache.get(callId)));
                                    phoneNumber.setActive(true);
                                    phoneNumber.setTelephoneProvider(TelephoneProvider.TELE_CMI);

                                    callDetails.setUserNumber(phoneNumber);
                                    receiverCache.remove(callId);
                                }

                                callSummary.setOrgId(orgId);
                                callSummary.setStartTime(new DateTime(currentTime));
                                callSummary.setEndTime(new DateTime(currentTime));
                                callSummary.setDuration(ZERO);
                                callSummary.setBillingDuration(ZERO);

                                callDetails.setSummary(callSummary);
                                callDetails.setOrgId(orgId);
                                callDetails.setExternalId(callId);
                                callDetails.setType(callType);
                                callDetails.setContactNumber(phone);
                                callDetails.setStatus(callStatus);
                                callDetails.setParticipantUser(crmUser);

                                final String number = PhoneNumberUtil.parse(phone.getNumberWithCode()).getNumberWithCountryCode();
                                final Collection<Contact> existingContacts = contactManager.getContactsByPhone(orgId, number);

                                if (!existingContacts.isEmpty()) {
                                    setExistingEntities(orgId, callDetails, existingContacts);
                                } else if (phoneSettings.isAutoCreateContactEnabled()) {
                                    final Contact contact = createContactWithPreferredStage(orgId, crmUser.id(), addCountryCodePrefix(caller), phoneSettings);

                                    if (Objects.nonNull(contact.id())) {
                                        callDetails.setContact(contact);
                                    }
                                } else {
                                    callDetails.setContact(null);
                                }

                                final Collection<ErrorDescriptor> errorDescriptors = callDetailsValidator.validate(List.of(callDetails), Group.Create.class);
                                final Collection<String> violations = errorDescriptors.stream().map(ErrorDescriptor::message).toList();

                                if (violations.isEmpty()) {
                                    callHistoryManager.save(callDetails);
                                } else {
                                    LOGGER.warn("Call History: Create Violations: {}", crmResponse.resolveI18nString(violations));
                                    crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(NotificationErrorCodes.CALL_HISTORY_CREATE_FAILED)), errorDescriptors));
                                }
                            });
                        }
                    }
                }
            }
            default -> callHistoryManager.updateCallStatus(callId, callStatus);
        }
    }

    /**
     * Builds a {@link Phone} for the given organization, receiver number, and optional contact number ID.
     *
     * @param orgId           the organization identifier
     * @param contactNumber   the contact phone number
     * @param contactNumberId the contact number ID, or null to create a new phone record
     * @return the constructed {@link Phone} instance
     */
    private Phone buildPhone(final Long orgId, final String contactNumber, final String contactNumberId) {
        final String phoneNumber = addCountryCodePrefix(contactNumber);

        if (Objects.nonNull(contactNumberId)) {
            final Phone phone = new Phone();

            phone.setId(Long.parseLong(contactNumberId));
            phone.setOrgId(orgId);
            return phone;
        } else if (phoneManager.exists(orgId, phoneNumber)) {
            return phoneManager.get(orgId, phoneNumber);
        } else {
            final Phone phone = new Phone();
            final com.tlc.l10n.model.number.PhoneNumber number = PhoneNumberUtil.parse(phoneNumber);

            phone.setOrgId(orgId);
            phone.setNumber(phoneNumber);
            phone.setType(Phone.Type.MOBILE);
            phone.setCountry(number.getCountry());

            return phoneManager.create(phone);
        }
    }

    /**
     * <p>
     * Updates an incoming call, which was saved as missed by an agent who did not answer it, with the agent who answered it.
     * </p>
     *
     * @param callDetails Represents the already saved {@link CallDetails}.
     * @param crmUser     Represents the {@link CrmUser} who answered the call.
     * @param webhookData Represents the {@link CallStatusWebhookData} of the answered call.
     * @param crmResponse Represents the {@link CrmResponse} for error reporting.
     */
    private void updateIncomingCallAsAnswered(final CallDetails callDetails, final CrmUser crmUser, final CallStatusWebhookData webhookData, final CrmResponse crmResponse) {
        final Long orgId = callDetails.orgId();
        final String duration = webhookData.getDuration();
        final Long startTime = webhookData.getStartTime();
        final long currentTime = Instant.now().toEpochMilli();
        final CallSummary callSummary = new CallSummary();

        callSummary.setOrgId(orgId);
        callSummary.setStartTime(new DateTime(startTime));
        callSummary.setEndTime(new DateTime(currentTime));
        callSummary.setDuration(Objects.isNull(duration) ? String.valueOf((currentTime - startTime) / 1000) : duration);
        callSummary.setBillingDuration(ZERO);

        callDetails.setSummary(callSummary);
        callDetails.setParticipantUser(crmUser);
        callDetails.setStatus(webhookData.getCallStatus());
        loadCallRecordingIntoCallDetails(orgId, callDetails, webhookData);

        final Collection<ErrorDescriptor> errorDescriptors = callDetailsValidator.validate(List.of(callDetails), Group.Update.class);
        final Collection<String> violations = errorDescriptors.stream().map(ErrorDescriptor::message).toList();

        if (violations.isEmpty()) {
            callHistoryManager.update(callDetails);
        } else {
            LOGGER.warn("Call History: Update Violations: {}", crmResponse.resolveI18nString(violations));
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(NotificationErrorCodes.CALL_HISTORY_UPDATE_FAILED)), errorDescriptors));
        }
    }

    /**
     * Ensures that the given phone number includes a country code prefix ('+').
     *
     * @param number the phone number to validate or modify
     * @return the phone number with a '+' country code prefix
     */
    private String addCountryCodePrefix(final String number) {
        if (Objects.isNull(number)) {
            return null;
        }
        return number.startsWith(PLUS) ? number : String.join(StringUtil.EMPTY_STRING, PLUS, number);
    }

    /**
     * Loads call recording details into the call history based on webhook data.
     *
     * @param orgId                 the organization identifier
     * @param callDetails           the {@link CallDetails} to populate with recording info
     * @param callStatusWebhookData the webhook payload containing call recording information
     */
    private void loadCallRecordingIntoCallDetails(final Long orgId, final CallDetails callDetails, final CallStatusWebhookData callStatusWebhookData) {
        final String recordFileName = callStatusWebhookData.getRecordFileName();

        if (Objects.isNull(recordFileName)) {
            return;
        }
        final Optional<TeleCMIAuthData> authData = teleCMIActionManager.getAuthData(orgId, null);

        authData.ifPresent(teleCMIAuthData -> {
            final String recordedURL = teleCMIService.buildVoiceRecordUrl(teleCMIAuthData, recordFileName);
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
     * Sets the existing contacts and deals in call details.
     *
     * @param orgId                 the organization identifier
     * @param callDetails           the {@link CallDetails} to populate with recording info
     * @param existingContacts      the collection of existing of contacts with the number
     */
    private void setExistingEntities(final Long orgId, final CallDetails callDetails, final Collection<Contact> existingContacts) {
        final Contact contact = existingContacts.stream().filter(existingContact -> Objects.equals(existingContact.getPrimaryPhone().getNumber(),
                        PhoneNumberUtil.parse(callDetails.getContactNumber().getNumberWithCode()).getNumber())).findFirst().orElse(null);

        if (Objects.nonNull(contact)) {
            callDetails.setContact(contact);

            final Deal deal = dealManager.findSingleDealByContact(orgId, contact.id()).orElse(null);

            if (Objects.nonNull(deal)) {
                callDetails.setDeal(deal);
            }
        }
    }
}

