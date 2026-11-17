package com.tlc.otp.service.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.crypto.HashGenerator;
import com.tlc.otp.code.OTPErrorCodes;
import com.tlc.otp.manager.OTPRequestManager;
import com.tlc.otp.model.OTPRequest;
import com.tlc.otp.model.OTPConfiguration;
import com.tlc.otp.service.OTPGenerator;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;

import jakarta.validation.ConstraintViolation;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Implementation of the {@link OTPGenerator} provides a standard method for generating OTPs using a given configuration
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class OTPGeneratorImpl implements OTPGenerator {

    private final HashGenerator hashGenerator;
    private final OTPRequestManager otpRequestManager;

    private OTPGeneratorImpl() {
        hashGenerator = HashGenerator.getInstance();
        otpRequestManager = OTPRequestManager.getInstance();
    }

    private static class Instance {
        private static final OTPGeneratorImpl INSTANCE = new OTPGeneratorImpl();
    }

    /**
     * <p>
     * Fetches the {@link OTPGeneratorImpl} instance
     * </p>
     *
     * @return The {@link OTPGeneratorImpl} instance
     */
    public static OTPGeneratorImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param otpRequest       Represents the {@link OTPRequest}
     * @param otpConfiguration The {@link OTPConfiguration}
     */
    @Override
    public void generate(final OTPRequest otpRequest, final OTPConfiguration otpConfiguration) {
        final String uniqueId = String.format("%s%s%d", otpRequest.getReceiver(),
                otpRequest.getCategory().getName(), Instant.now().toEpochMilli());
        final String otp = hashGenerator.getRandomUniqueId(uniqueId, otpConfiguration.digit(),
                Objects.equals(otpConfiguration.type(), OTPConfiguration.Type.NUMERIC));
        final String verificationToken = hashGenerator.generateHash(uniqueId);

        if (Objects.nonNull(otp)) {
            otpRequest.setOtp(otp);
            otpRequest.setVerificationToken(verificationToken);
            final Set<ConstraintViolation<OTPRequest>> violations = ValidatorServiceProvider.get().modelValidator()
                    .validate(otpRequest, Group.Update.class);

            if (!violations.isEmpty()) {
                for (final ConstraintViolation<OTPRequest> violation : violations) {
                    throw ErrorCode.get(OTPErrorCodes.VALIDATION_FAILED, violation.getMessage());
                }
            }
            otpRequestManager.save(otpRequest);
        }
    }
}
