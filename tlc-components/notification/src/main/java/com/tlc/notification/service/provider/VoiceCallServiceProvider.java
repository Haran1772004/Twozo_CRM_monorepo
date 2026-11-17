package com.tlc.notification.service.provider;

import com.tlc.notification.model.call.CallRoutingDetails;
import com.tlc.notification.model.call.CallType;
import com.tlc.notification.model.call.CallerToken;
import com.tlc.notification.model.call.AvailableCountries;
import com.tlc.notification.model.call.AvailableLocalities;
import com.tlc.notification.model.call.AvailableNumbers;
import com.tlc.notification.model.call.AvailableStates;
import com.tlc.notification.model.call.MobileNumber;
import com.tlc.notification.model.call.NumberCriteria;
import com.tlc.notification.model.call.Recipient;
import com.tlc.notification.model.call.VoiceCall;

import java.net.URI;

/**
 * <p>
 * Provides the call services.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface VoiceCallServiceProvider {

    /**
     * <p>
     * Fetches the {@link AvailableCountries} from the provider.
     * </p>
     *
     * @param numberCriteria Represents the criteria to fetch countries.
     * @return {@link AvailableCountries}.
     */
    AvailableCountries fetchAvailableCountries(final NumberCriteria numberCriteria);

    /**
     * <p>
     * Fetches the {@link AvailableStates} from the provider.
     * </p>
     *
     * @param numberCriteria Represents the criteria to fetch states.
     * @return {@link AvailableStates}.
     */
    AvailableStates fetchAvailableStates(final NumberCriteria numberCriteria);

    /**
     * <p>
     * Fetches the {@link AvailableLocalities} from the provider.
     * </p>
     *
     * @param numberCriteria Represents the criteria to fetch localities.
     * @return {@link AvailableLocalities}.
     */
    AvailableLocalities fetchAvailableLocalities(final NumberCriteria numberCriteria);

    /**
     * <p>
     * Fetches the {@link AvailableNumbers} from the provider.
     * </p>
     *
     * @param numberCriteria Represents the criteria to fetch numbers.
     * @return {@link AvailableNumbers}.
     */
    AvailableNumbers fetchAvailableNumbers(final NumberCriteria numberCriteria);

    /**
     * <p>
     * Buys the {@link MobileNumber} from the service provider.
     * </p>
     *
     * @param mobileNumber Represents the number to be bought.
     */
    void buyMobileNumber(final MobileNumber mobileNumber);

    /**
     * <p>
     * Updates the name {@link MobileNumber} in the service provider.
     * </p>
     *
     * @param mobileNumber Represents the number to be updated.
     */
    void updateMobileNumberName(final MobileNumber mobileNumber);

    /**
     * <p>
     * Deletes the {@link MobileNumber} in the service provider.
     * </p>
     *
     * @param mobileNumber Represents the number to be deleted.
     */
    void deleteNumber(final MobileNumber mobileNumber);

    /**
     * <p>
     * Generates the token for initiating the WebRTC calling.
     * </p>
     *
     * @param identity Represents the identity of the token to be generated.
     * @return {@link CallerToken}.
     */
    CallerToken generateToken(final String identity);

    /**
     * Routes a WebRTC call based on the recipient and call type.
     *
     * <p>
     * This method determines how a call should be routed based on:
     * - The {@link Recipient} identity (e.g., agent or customer).
     * - The {@link CallType}, specifying whether the call is INCOMING or OUTGOING.
     * </p>
     *
     * <p>
     * For incoming calls, this method ensures the call is directed to the appropriate agent.
     * For outgoing calls, it connects the agent to the specified customer.
     * </p>
     *
     * @param callRoutingDetails Holds the details of the call.
     * @return A string indicating the call routing status or session ID.
     */
    String routeCall(final CallRoutingDetails callRoutingDetails);

    /**
     * Transfers a call to the given recipients.
     *
     * @param callRoutingDetails Holds the details of the call.
     */
    void transferCall(final CallRoutingDetails callRoutingDetails);

    /**
     * Enables/disables the call recordings.
     *
     * @param callId Holds the ID of the call.
     * @param needCallRecord Represents the needs of the call recording.
     */
    void recordCall(final String callId, final boolean needCallRecord);

    /**
     * <p>
     * Fetches the parent call ID of the given call ID.
     * </p>
     *
     * @param childCallId Represents the child call ID.
     * @return Parent call ID.
     */
    String getParentCallId(final String childCallId);

    /**
     * <p>
     * Makes the voiceCall to the given receiver number.
     * </p>
     *
     * @param voiceCall an entity that holds the voiceCall attributes
     */
    void makeCall(final VoiceCall voiceCall);

    /**
     * <p>
     * Validates the sender number for initiate social communication.
     * </p>
     *
     * @param number a number which is need to validate
     * @return validation code for verify the given number
     */
    String validateNumber(final MobileNumber number, final URI callBackUri);

    /**
     * <p>
     * Redirects the call into the given receiver number.
     * </p>
     *
     * @param receiverNumber a number which will be received the redirected call
     * @return XML voice response.
     */
    String redirectCall(final MobileNumber receiverNumber);

    /**
     * <p>
     * Fetches the call details by using call sid.
     * </p>
     *
     * @param callId for fetch the call details
     * @return {@link VoiceCall} object
     */
    VoiceCall fetchCallDetails(final String callId);

    /**
     * <p>
     * Fetches the call status by using call sid.
     * </p>
     *
     * @param callId for fetch the call status
     * @return call status
     */
    String fetchCallStatus(final String callId);

    /**
     * <p>
     * Stops the external service client by assigning null value.
     * </p>
     */
    void stop();
}
