package com.tlc.otp.service.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.otp.code.OTPErrorCodes;
import com.tlc.otp.manager.OTPRequestManager;
import com.tlc.otp.model.OTPRequest;
import com.tlc.otp.service.OTPValidator;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;

import jakarta.validation.ConstraintViolation;

import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Implements the {@link OTPValidator} and handles the validation of one-time passwords
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class OTPValidatorImpl implements OTPValidator {

    private final OTPRequestManager otpRequestManager;

    private OTPValidatorImpl() {
        otpRequestManager = OTPRequestManager.getInstance();
    }

    private static class Instance {
        private static final OTPValidatorImpl INSTANCE = new OTPValidatorImpl();
    }

    /**
     * <p>
     * Fetches the {@link OTPValidatorImpl} instance
     * </p>
     *
     * @return The {@link OTPValidatorImpl} instance
     */
    public static OTPValidatorImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param otpRequest Represents the {@link OTPRequest}
     * @return true if the provided OTP is valid, false otherwise
     */
    @Override
    public boolean validate(final OTPRequest otpRequest) {
        final Set<ConstraintViolation<OTPRequest>> violations = ValidatorServiceProvider.get().modelValidator().validate(otpRequest, Group.Update.class);

        if (!violations.isEmpty()) {
            for (final ConstraintViolation<OTPRequest> violation : violations) {
                throw ErrorCode.get(OTPErrorCodes.VALIDATION_FAILED, violation.getMessage());
            }
        }
        final boolean isValidOtp = otpRequestManager.isValidOTP(otpRequest);

        if (isValidOtp) {
            otpRequestManager.updateVerifiedTime(otpRequest.getReceiver(), otpRequest.getChannel(), otpRequest.getCategory());
        }
        return isValidOtp;
    }

    /**
     * {@inheritDoc}
     *
     * @param otpRequest Represents the {@link OTPRequest}
     * @param verificationToken The token to validate
     * @return true if the provided token is valid, false otherwise
     */
    @Override
    public boolean verifyToken(final OTPRequest otpRequest, final String verificationToken) {
        return Objects.equals(otpRequest.getVerificationToken(), verificationToken) && Objects.isNull(otpRequest.getVerifiedAt());
    }
}
