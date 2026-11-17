package com.tlc.notification.internal.config;

import com.tlc.commons.code.ErrorCode;

import com.tlc.notification.model.call.AvailableCountries;
import com.tlc.notification.model.call.AvailableLocalities;
import com.tlc.notification.model.call.AvailableNumbers;
import com.tlc.notification.model.call.AvailableStates;
import com.tlc.notification.model.call.CallRoutingDetails;
import com.tlc.notification.model.call.CallerToken;
import com.tlc.notification.model.call.MobileNumber;
import com.tlc.notification.model.call.NumberCriteria;
import com.tlc.notification.service.provider.VoiceCallServiceProvider;
import com.tlc.notification.service.notification.VoiceCallService;
import com.tlc.notification.internal.listener.*;
import com.tlc.notification.internal.status.NotificationErrorCodes;
import com.tlc.notification.model.call.VoiceCall;
import com.tlc.notification.listeners.NotificationType;
import com.tlc.notification.model.Notification;

import java.net.URI;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * <p>
 * Provides the voice call service.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class VoiceCallServiceImpl implements VoiceCallService {

    private final VoiceCallServiceProvider serviceProvider;

    public VoiceCallServiceImpl(final VoiceCallServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(final VoiceCall voiceCall) {
        try {
            beforeSend(voiceCall);
            serviceProvider.makeCall(voiceCall);
            onSuccess(voiceCall);
        } catch (Throwable throwable) {
            onFailure(voiceCall);
            throw ErrorCode.get(NotificationErrorCodes.UNABLE_TO_MAKE_CALL, throwable);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param numberCriteria Represents the criteria to fetch countries.
     * @return {@link AvailableCountries}.
     */
    @Override
    public AvailableCountries fetchAvailableCountries(final NumberCriteria numberCriteria) {
        return serviceProvider.fetchAvailableCountries(numberCriteria);
    }

    /**
     * {@inheritDoc}
     *
     * @param numberCriteria Represents the criteria to fetch states.
     * @return {@link AvailableStates}.
     */
    @Override
    public AvailableStates fetchAvailableStates(final NumberCriteria numberCriteria) {
        return serviceProvider.fetchAvailableStates(numberCriteria);
    }

    /**
     * {@inheritDoc}
     *
     * @param numberCriteria Represents the criteria to fetch localities.
     * @return {@link AvailableLocalities}.
     */
    @Override
    public AvailableLocalities fetchAvailableLocalities(final NumberCriteria numberCriteria) {
        return serviceProvider.fetchAvailableLocalities(numberCriteria);
    }

    /**
     * {@inheritDoc}
     *
     * @param numberCriteria Represents the criteria to fetch numbers.
     * @return {@link AvailableNumbers}.
     */
    @Override
    public AvailableNumbers fetchAvailableNumbers(final NumberCriteria numberCriteria) {
        return serviceProvider.fetchAvailableNumbers(numberCriteria);
    }

    /**
     * {@inheritDoc}
     *
     * @param mobileNumber Represents the number to be bought.
     */
    @Override
    public void buyMobileNumber(final MobileNumber mobileNumber) {
        serviceProvider.buyMobileNumber(mobileNumber);
    }

    /**
     * {@inheritDoc}
     *
     * @param mobileNumber Represents the number to be updated.
     */
    @Override
    public void updateMobileNumberName(final MobileNumber mobileNumber) {
        serviceProvider.updateMobileNumberName(mobileNumber);
    }

    /**
     * {@inheritDoc}
     *
     * @param mobileNumber Represents the number to be deleted.
     */
    @Override
    public void deleteNumber(final MobileNumber mobileNumber) {
        serviceProvider.deleteNumber(mobileNumber);
    }

    /**
     * {@inheritDoc}
     *
     * @param identity Represents the identity of the token to be generated.
     * @return {@link CallerToken}.
     */
    @Override
    public CallerToken generateToken(final String identity) {
        return serviceProvider.generateToken(identity);
    }

    /**
     * {@inheritDoc}
     *
     * @param callRoutingDetails Holds the details of the call.
     * @return A string indicating the call routing status or session ID.
     */
    @Override
    public String routeCall(final CallRoutingDetails callRoutingDetails) {
        return serviceProvider.routeCall(callRoutingDetails);
    }

    /**
     * {@inheritDoc}
     *
     * @param callRoutingDetails Holds the details of the call.
     */
    public void transferCall(final CallRoutingDetails callRoutingDetails) {
        serviceProvider.transferCall(callRoutingDetails);
    }

    /**
     * {@inheritDoc}
     *
     * @param callId         Holds the ID of the call.
     * @param needCallRecord Represents the needs of the call recording.
     */
    @Override
    public void recordCall(final String callId, final boolean needCallRecord) {
        serviceProvider.recordCall(callId, needCallRecord);
    }

    /**
     * {@inheritDoc}
     *
     * @param childCallId Represents the child call ID.
     * @return Parent call ID.
     */
    @Override
    public String getParentCallId(final String childCallId) {
        return serviceProvider.getParentCallId(childCallId);
    }

    /**
     * <p>
     * Validates the sender number for initiate social communication.
     * </p>
     *
     * @param number a number which is need to validate.
     * @return validation code for verify the given number.
     */
    @Override
    public String validateNumber(final MobileNumber number, final URI callBackUri) {
        return serviceProvider.validateNumber(number, callBackUri);
    }

    /**
     * <p>
     * Redirects the call into the given receiver number.
     * </p>
     *
     * @param receiverNumber a number which will be received the redirected call.
     * @return XML voice response.
     */
    @Override
    public String redirectCall(final MobileNumber receiverNumber) {
        return serviceProvider.redirectCall(receiverNumber);
    }

    /**
     * <p>
     * Fetches the call details by using call sid.
     * </p>
     *
     * @param callId for fetch the call details.
     * @return {@link VoiceCall} object.
     */
    @Override
    public VoiceCall fetchCallDetails(final String callId) {
        return serviceProvider.fetchCallDetails(callId);
    }

    /**
     * <p>
     * Fetches the call status by using call sid.
     * </p>
     *
     * @param callId for fetch the call status.
     * @return call status.
     */
    @Override
    public String fetchCallStatus(final String callId) {
        return serviceProvider.fetchCallStatus(callId);
    }

    /**
     * <p>
     * Stops the external service client by assigning null value.
     * </p>
     */
    @Override
    public void stop() {
        serviceProvider.stop();
    }

    /**
     * <p>
     * Listens the {@link VoiceCall} on aborted.
     * </p>
     *
     * @param voiceCall {@link VoiceCall} object for listening.
     */
    private void onFailure(final VoiceCall voiceCall) {
        new ErrorHandler().handle(constructDataMap(voiceCall));
    }

    /**
     * <p>
     * Listens the {@link VoiceCall} on before send.
     * </p>
     *
     * @param voiceCall {@link VoiceCall} object for listening.
     */
    private void beforeSend(final VoiceCall voiceCall) {
        new PreWorkHandler().handle(constructDataMap(voiceCall));
    }

    /**
     * <p>
     * Listens the {@link VoiceCall} on after sent.
     * </p>
     *
     * @param voiceCall {@link VoiceCall} object for listening.
     */
    private void onSuccess(final VoiceCall voiceCall) {
        new SuccessHandler().handle(constructDataMap(voiceCall));
    }

    /**
     * <p>
     * Constructs the data map for listening purpose.
     * </p>
     *
     * @param voiceCall {@link VoiceCall} object for data map construction.
     * @return {@link SortedMap}
     */
    private SortedMap<NotificationType, List<Notification<?, ?>>> constructDataMap(final VoiceCall voiceCall) {
        final SortedMap<NotificationType, List<Notification<?, ?>>> dataMap = new TreeMap<>();

        dataMap.put(NotificationType.CALL, List.of(voiceCall));
        return dataMap;
    }
}
