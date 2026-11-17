package com.tlc.notification.service.telephony.mcube;

import com.tlc.commons.service.Service;

import com.tlc.notification.internal.mcube.MCubeServiceImpl;
import com.tlc.notification.model.telephone.TelephoneResponse;
import com.tlc.notification.model.telephone.mcube.MCubeAuthData;
import com.tlc.notification.model.telephone.mcube.MCubeUser;

import java.net.URL;

/**
 * An MCube-specific telephony service interface.
 *
 * @author Sathish
 * @version 1.0
 */
public interface MCubeService extends Service {

    /**
     * Gets the {@link MCubeService} instance.
     *
     * @return The instance of {@link MCubeService}
     */
    static MCubeService getInstance() {
        return MCubeServiceImpl.getInstance();
    }

    /**
     * Authenticates with the MCube provider using the specified authentication details.
     *
     * @param mCubeAuthData the authentication details required to connect with MCube
     * @return the authentication response from MCube
     */
    TelephoneResponse authenticate(final MCubeAuthData mCubeAuthData);

    /**
     * Creates a new user (agent) in the MCube provider.
     *
     * @param mCubeUser the user details to be created in MCube
     * @return the creation response from MCube
     */
    TelephoneResponse createUser(final MCubeUser mCubeUser);

    /**
     * Updates an existing user in the MCube provider.
     *
     * @param mCubeUser the updated user details to be synchronized with MCube
     * @return the update response from MCube
     */
    TelephoneResponse updateUser(final MCubeUser mCubeUser);

    /**
     * Removes an existing user from the MCube provider.
     *
     * @param mCubeUser the user details identifying the account to be removed
     * @return the deletion response from MCube
     */
    TelephoneResponse removeUser(final MCubeUser mCubeUser);

    /**
     * Initiates a click-to-call via MCube.
     *
     * @param mCubeAuthData  the authentication details
     * @param agentNumber    the agent's phone number
     * @param customerNumber the customer's phone number
     * @return the call initiation response
     */
    TelephoneResponse initiateCall(final MCubeAuthData mCubeAuthData, final String agentNumber, final String customerNumber);

    /**
     * Gets the configured webhook URL for MCube callbacks.
     *
     * <p>This URL is used by MCube to send event notifications such as call status updates,
     * call recordings, and other telephony events back to the CRM system.</p>
     *
     * @return the webhook {@link URL} configured for MCube event callbacks
     */
    URL getWebhookURL();

    /**
     * <p>
     * Builds the record file URL for the given MCube authentication details and call id.
     * </p>
     *
     * @param mCubeAuthData Represents the {@link MCubeAuthData}
     * @param callId        The MCube call id for which the recording URL is required
     * @return The accessible URL of the recorded file as a {@link String}
     */
    String buildVoiceRecordUrl(final MCubeAuthData mCubeAuthData, final String callId);
}

