package com.tlc.crm.notifications.action.call;

import com.tlc.commons.data.DataProcessorProvider;
import com.tlc.commons.data.model.RequestData;
import com.tlc.commons.data.processor.DataProcessor;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.StringUtil;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.pub.CrmPublicAction;
import com.tlc.crm.common.action.res.CrmXmlResponse;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;

import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.entity.models.type.Phone;

import com.tlc.crm.notifications.api.call.manager.CallHistoryManager;
import com.tlc.crm.notifications.api.call.manager.CallManager;
import com.tlc.crm.notifications.api.call.manager.PhoneNumberManager;
import com.tlc.crm.notifications.api.call.model.CallDetails;
import com.tlc.crm.notifications.api.call.model.InboundDetails;
import com.tlc.crm.notifications.api.call.model.PhoneNumber;
import com.tlc.crm.notifications.fields.NotificationFields;
import com.tlc.crm.notifications.validator.CallDetailsValidator;

import com.tlc.crm.security.context.SecureTaskProcessor;

import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.model.number.PhoneNumberUtil;

import com.tlc.notification.model.call.CallRoutingDetails;
import com.tlc.notification.model.call.CallStatus;
import com.tlc.notification.model.call.CallType;
import com.tlc.notification.model.call.MobileNumber;
import com.tlc.notification.model.call.Parameters;
import com.tlc.notification.model.call.Recipient;
import com.tlc.notification.service.notification.VoiceCallService;
import com.tlc.notification.service.provider.NotificationServiceProvider;
import com.tlc.notification.webhook.model.CallHandleWebhookData;

import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.type.Group;

import com.tlc.web.WebAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Handles inbound and outbound call management for the CRM notification system.
 *
 * <p>This class is responsible for processing WebRTC and PSTN calls by interacting with
 * {@link VoiceCallService} and {@link CallManager} to determine call routing.</p>
 *
 * <p>Supported call types:</p>
 * <ul>
 *     <li><b>INCOMING</b> – Routes customer calls to the appropriate agent.</li>
 *     <li><b>OUTGOING</b> – Initiates calls from an agent to a customer.</li>
 * </ul>
 *
 * @author Sathish
 * @version 1.0
 */
@WebAction(path = "/{provider}/call/handle/mgmt")
public class CallHandler extends CrmPublicAction implements SecureTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallHandler.class);
    private static final String EMPTY_STRING = "";
    private static final String PLUS = "+";

    private final VoiceCallService voiceCallService;
    private final CallManager callManager;
    private final PhoneNumberManager phoneNumberManager;
    private final ContactManager contactManager;
    private final CallHistoryManager callHistoryManager;
    private final PhoneManager phoneManager;
    private final UserService userService;
    private final CallDetailsValidator callDetailsValidator;
    private final DealManager dealManager;

    public CallHandler() {
        voiceCallService = NotificationServiceProvider.get().getVoiceCallService();
        callManager = CallManager.getInstance();
        phoneNumberManager = PhoneNumberManager.getInstance();
        contactManager = ContactManager.getInstance();
        callHistoryManager = CallHistoryManager.getInstance();
        phoneManager = PhoneManager.getInstance();
        userService = UserService.getInstance();
        callDetailsValidator = CallDetailsValidator.getInstance();
        dealManager = DealManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     The CRM request containing call parameters.
     * @param crmResponse The CRM response to store the call routing result.
     */
    @Override
    public void pProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final String providerName = request.getParameter(NotificationFields.PROVIDER.getName());

        if (Objects.nonNull(providerName)) {
            final String webhookPath = StringUtil.join(StringUtil.Delimiter.UNDER_SCORE, providerName,
                    NotificationFields.CALL_HANDLE.getName());

            final DataProcessor<CallHandleWebhookData> webhookProcessor = DataProcessorProvider.getProcessor(webhookPath);
            final RequestData<JsonObject> webhookRequest = new RequestData<>(request.getRequestJson(), request.getParams());
            final CrmXmlResponse crmXmlResponse = crmResponse.convertTo(CrmXmlResponse.class);

            webhookProcessor.process(webhookRequest, webhookData -> {
                final IOCallData ioCallData = buildIOCallData(webhookData);
                final Predicate<CallDetails> violationsChecker = callDetails -> {
                    final Collection<String> violations = callDetailsValidator.validate(List.of(callDetails), Group.Create.class)
                            .stream().map(ErrorDescriptor::message).collect(Collectors.toSet());

                    if (violations.isEmpty()) {
                        return true;
                    } else {
                        LOGGER.warn("Call History: Create Violations: {}", crmResponse.resolveI18nString(violations));
                        return false;
                    }
                };
                final Locale locale = request.locale();
                final Locale currentLocale = Objects.isNull(locale) ? Locale.US : locale;
                final String voiceResponse;

                if (Objects.isNull(ioCallData) || !violationsChecker.test(ioCallData.callDetails)) {
                    final CallRoutingDetails callRoutingDetails = new CallRoutingDetails();

                    callRoutingDetails.setCallerLocale(currentLocale);
                    voiceResponse = voiceCallService.routeCall(callRoutingDetails);
                } else {
                    final CallRoutingDetails callRoutingDetails = ioCallData.callRoutingDetails;

                    callRoutingDetails.setCallerLocale(currentLocale);
                    voiceResponse = voiceCallService.routeCall(callRoutingDetails);

                    callHistoryManager.save(ioCallData.callDetails);
                }
                crmXmlResponse.put(NotificationFields.VOICE_RESPONSE.getName(), voiceResponse);
            });
            crmXmlResponse.send();
        }
    }

    /**
     * Builds a {@link IOCallData} based on the given request.
     *
     * @param webhookData The {@link CallHandleWebhookData} containing call details.
     * @return {@link IOCallData} objects to route and store the call.
     */
    private IOCallData buildIOCallData(final CallHandleWebhookData webhookData) {
        final CallType callType = webhookData.getCallType();
        final String receiver = webhookData.getReceiver();
        final String callId = webhookData.getCallId();
        final CallRoutingDetails callRoutingDetails = new CallRoutingDetails();
        final CallDetails callDetails = new CallDetails();
        final Collection<Recipient> recipients = new ArrayList<>();
        final Parameters parameters = new Parameters();

        parameters.addParam(NotificationFields.CALL_TYPE.getName(), callType.getName());
        callDetails.setExternalId(callId);
        callDetails.setType(callType);
        callDetails.setParentCall(true);
        callDetails.setStatus(CallStatus.YET_TO_CONNECT);
        callRoutingDetails.setCallId(callId);
        callRoutingDetails.setCallType(callType);
        callRoutingDetails.setRecipients(recipients);
        callRoutingDetails.setCallbackParams(parameters);

        return switch (callType) {
            case OUTGOING -> {
                final String fromNumber = webhookData.getCaller();
                final JsonObject customParams = webhookData.getCustomParams();
                final long callerId = customParams.getLong(NotificationFields.CALLER_ID.getName());
                final Long contactId = getNullableLong(customParams, NotificationFields.CONTACT_ID.getName());
                final Long dealId = getNullableLong(customParams, NotificationFields.DEAL_ID.getName());
                final Long contactNumberId = getNullableLong(customParams, NotificationFields.CONTACT_NUMBER_ID.getName());
                final String callerName = webhookData.getCallerName();
                final Optional<PhoneNumber> optionalPhoneNumber = phoneNumberManager.getNumber(fromNumber);

                if (optionalPhoneNumber.isPresent()) {
                    final PhoneNumber phoneNumber = optionalPhoneNumber.get();
                    final Long orgId = phoneNumber.orgId();
                    final CrmUser user = userService.getUser(orgId, callerId);

                    secureProcess(user, () -> {
                        final MobileNumber mobileNumber = new MobileNumber(fromNumber);
                        final Phone phone = new Phone();

                        mobileNumber.setFriendlyName(callerName);
                        callRoutingDetails.setCaller(mobileNumber);
                        callDetails.setOrgId(orgId);
                        callDetails.setUserNumber(phoneNumber);
                        callDetails.setParticipantUser(user);
                        recipients.add(new Recipient(receiver));

                        if (Objects.nonNull(contactId)) {
                            final Contact contact = new Contact();

                            contact.setId(contactId);
                            contact.setOrgId(orgId);
                            callDetails.setContact(contact);
                            dealManager.findSingleDealByContact(orgId, contactId).ifPresent(callDetails::setDeal);
                        }

                        if (Objects.nonNull(dealId)) {
                            final Deal deal = new Deal();

                            deal.setId(dealId);
                            callDetails.setDeal(deal);
                        }

                        if (Objects.isNull(contactNumberId)) {
                            final com.tlc.l10n.model.number.PhoneNumber number = PhoneNumberUtil.parse(receiver);
                            final Optional<Contact> savedContact = Objects.isNull(contactId) ? contactManager.getContactsByPhone(orgId, number.getNumber())
                                    .stream().filter(contact -> Objects.nonNull(contact.getPrimaryPhone())).findFirst() : Optional.empty();

                            if (savedContact.isPresent()) {
                                final Contact contact = savedContact.get();

                                contact.setOrgId(orgId);
                                callDetails.setContact(contact);
                                dealManager.findSingleDealByContact(orgId, contact.id()).ifPresent(callDetails::setDeal);
                                callDetails.setContactNumber(contact.getPrimaryPhone());
                            } else {
                                phone.setOrgId(orgId);
                                phone.setNumber(number.hasCountryCode() ? addCountryCodePrefix(number.getNumberWithCountryCode()) : number.getNumber());
                                phone.setType(Phone.Type.MOBILE);
                                phone.setCountry(number.getCountry());
                                phoneManager.create(phone);
                                callDetails.setContactNumber(phone);
                            }
                        } else {
                            phone.setId(contactNumberId);
                            phone.setOrgId(orgId);
                            callDetails.setContactNumber(phone);
                        }
                    });
                    yield new IOCallData(callDetails, callRoutingDetails);
                }
                yield null;
            }
            case INCOMING -> {
                final String caller = webhookData.getCaller();
                final Optional<PhoneNumber> optionalPhoneNumber = phoneNumberManager.getNumber(receiver);

                if (optionalPhoneNumber.isPresent()) {
                    final PhoneNumber phoneNumber = optionalPhoneNumber.get();
                    final Long orgId = phoneNumber.orgId();
                    final Country country = webhookData.getCallerCountry();

                    if (Objects.nonNull(country)) {
                        final String callerNumber = caller.replace(country.getCode(), EMPTY_STRING);
                        final Collection<Contact> contacts = contactManager.getContactsByPhone(orgId, callerNumber);
                        final InboundDetails inboundDetails = callManager.getInboundDetails(contacts, phoneNumber);
                        final CrmUser user = inboundDetails.getUsers().stream().findFirst().get();

                        secureProcess(user, () -> {
                            final MobileNumber mobileNumber = new MobileNumber(caller);
                            final Optional<Contact> optionalContact = contacts.stream().findFirst();

                            mobileNumber.setFriendlyName(inboundDetails.getContactName());
                            callRoutingDetails.setCaller(mobileNumber);
                            callDetails.setOrgId(orgId);
                            callDetails.setUserNumber(phoneNumber);

                            if (optionalContact.isPresent()) {
                                final Contact contact = optionalContact.get();

                                callDetails.setContact(contact);
                                callDetails.setContactNumber(contact.getPrimaryPhone());
                            } else {
                                final Phone phone = new Phone();

                                phone.setOrgId(orgId);
                                phone.setNumber(caller);
                                phone.setType(Phone.Type.MOBILE);
                                phone.setCountry(country);
                                phoneManager.create(phone);
                                callDetails.setContactNumber(phone);
                            }
                            inboundDetails.getUsers().stream().map(recipient -> TokenIdentityBuilder.getIdentity(recipient.orgId(), recipient.id()))
                                    .map(receiverIdentity -> new Recipient(receiverIdentity, inboundDetails.getReceiverName())).forEach(recipients::add);
                        });
                        yield new IOCallData(callDetails, callRoutingDetails);
                    }
                }
                yield null;
            }
        };
    }

    /**
     * Ensures the number has exactly one leading '+' to prevent duplicate country codes.
     *
     * @param number the number with country code
     * @return the normalized number
     */
    private String addCountryCodePrefix(final String number) {
        return number.startsWith(PLUS) ? number : PLUS.concat(number);
    }

    /**
     * Reads a numeric custom parameter as {@link Long}, converting it from a string if necessary.
     *
     * @param customParams the call webhook custom parameters
     * @param key the parameter key
     * @return the {@link Long} value, or {@code null} if unavailable
     */
    private Long getNullableLong(final JsonObject customParams, final String key) {
        final Object value = customParams.getNullableValue(key);

        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text.trim());
        }
        return null;
    }

    /**
     * <p>
     * Holds the details of the incoming/outgoing call.
     * </p>
     *
     * @param callDetails        Represents the call details to be stored.
     * @param callRoutingDetails Represents the call routing details to be routed.
     */
    private record IOCallData(CallDetails callDetails, CallRoutingDetails callRoutingDetails) {
    }
}
