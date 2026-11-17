package com.tlc.notification.service.telephony.telecmi;

import com.tlc.commons.service.Service;
import com.tlc.notification.internal.telecmi.TeleCMIServiceImpl;
import com.tlc.notification.model.telephone.TelephoneResponse;
import com.tlc.notification.model.telephone.telecmi.TeleCMIAuthData;

import com.tlc.notification.model.telephone.telecmi.TeleCMIUser;

/**
 * A TeleCMI-specific extension of the {@link Service}.
 *
 * @author kishore J
 * @version 1.0
 */
public interface TeleCMIService extends Service {

    /**
     * <p>
     * Gets the {@link TeleCMIService} instance
     * </p>
     *
     * @return The instance of {@link TeleCMIService}
     */
    static TeleCMIService getInstance() {
        return TeleCMIServiceImpl.getInstance();
    }

    /**
     * <p>
     * Authenticates with the TeleCMI provider using the specified authentication details
     * </p>
     *
     * @param telephoneAuthData the authentication details required to connect with the TeleCMI provider
     * @return the authentication response from the TeleCMI provider
     */
    TelephoneResponse authenticate(final TeleCMIAuthData telephoneAuthData);

    /**
     * <p>
     * Creates a new user in the TeleCMI provider
     * </p>
     *
     * @param teleCMIUser the user details to be created in the TeleCMI system
     * @return the creation response from the TeleCMI provider
     */
    TelephoneResponse createUser(final TeleCMIUser teleCMIUser);

    /**
     * <p>
     * Updates an existing user in the TeleCMI provider
     * </p>
     *
     * @param teleCMIUser the updated user details to be synchronized with the TeleCMI system
     * @return the update response from the TeleCMI provider
     */
    TelephoneResponse updateUser(final TeleCMIUser teleCMIUser);

    /**
     * <p>
     * Removes an existing user from the TeleCMI provider
     * </p>
     *
     * @param teleCMIUser the user details identifying the account to be removed
     * @return the deletion response from the TeleCMI provider
     */
    TelephoneResponse removeUser(final TeleCMIUser teleCMIUser);

    /**
     * <p>
     * Retrieves an existing user from the TeleCMI provider
     * </p>
     *
     * @param userId          the agent ID of the user to be retrieved
     * @param teleCMIAuthData the authentication details used to authorize the request
     * @return the {@link TeleCMIUser} populated from the TeleCMI provider response,
     * or {@code null} if the retrieval fails
     */
    TeleCMIUser getUser(final String userId, final TeleCMIAuthData teleCMIAuthData);

    /**
     * <p>
     * Builds the record file URL for the given TeleCMI authentication details and file name.
     * </p>
     *
     * @param teleCMIAuthenticationData Represents the {@link TeleCMIAuthData}
     * @param fileName                  The name of the recorded file for which the URL is required
     * @return The accessible URL of the recorded file as a {@link String}
     */
    String buildVoiceRecordUrl(final TeleCMIAuthData teleCMIAuthenticationData, final String fileName);
}
