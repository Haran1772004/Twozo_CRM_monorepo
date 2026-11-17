package com.tlc.otp.manager;

import com.tlc.otp.manager.impl.OTPRequestManagerImpl;
import com.tlc.otp.model.Category;
import com.tlc.otp.model.Channel;
import com.tlc.otp.model.OTPRequest;

import java.util.Optional;

/**
 * <p>
 * Manages the {@link OTPRequest} DB actions
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface OTPRequestManager {

    static OTPRequestManager getInstance() {
        return OTPRequestManagerImpl.getInstance();
    }

    /**
     * <p>
     * Saves the given {@link OTPRequest}
     * </p>
     *
     * @param otpRequest Represents the {@link OTPRequest}
     */
    void save(final OTPRequest otpRequest);

    /**
     * <p>
     * Fetches the {@link OTPRequest} based on the receiver, channel, and category
     * </p>
     *
     * @param receiver Represents the identifier of the receiver
     * @param channel  Represents the {@link Channel}
     * @param category Represents the {@link Category}
     * @return The {@link OTPRequest}
     */
    Optional<OTPRequest> get(final String receiver, final Channel channel, final Category category);

    /**
     * <p>
     * Creates a {@link Category} for the given {@link Category}
     * </p>
     *
     * @param category Represents the {@link Category}
     */
    void create(final Category category);

    /**
     * <p>
     * Fetches the {@link Category} for the given name
     * </p>
     *
     * @param applicationName Represents an application name
     * @param categoryName    Represents the name of the category
     * @return An Optional containing the {@link Category} if found, otherwise empty
     */
    Optional<Category> getCategory(final String applicationName, final String categoryName);

    /**
     * <p>
     * Validates the provided {@link OTPRequest}
     * </p>
     *
     * @param otpRequest Represents the {@link OTPRequest}
     * @return true if the OTP is valid, false otherwise
     */
    boolean isValidOTP(final OTPRequest otpRequest);

    /**
     * <p>
     * Updates the verified time for a receiver when their OTP verification is successful
     * </p>
     *
     * @param receiver Represents the identifier of the receiver
     * @param channel  Represents the {@link Channel}
     * @param category Represents the {@link Category}
     */
    void updateVerifiedTime(final String receiver, final Channel channel, final Category category);

    /**
     * <p>
     * Checks whether the receiver for a given receiver and category has already been verified
     * </p>
     *
     * @param receiver Represents the identifier of the receiver
     * @param channel  Represents the {@link Channel}
     * @param category Represents the {@link Category}
     * @return true if the channel is verified, false otherwise
     */
    boolean isReceiverVerified(final String receiver, final Channel channel, final Category category);

    /**
     * <p>
     * Updates the total attempts for the given {@link OTPRequest}
     * </p>
     *
     * @param otpRequest The {@link OTPRequest}
     */
    void updateTotalAttempts(final OTPRequest otpRequest);
}
