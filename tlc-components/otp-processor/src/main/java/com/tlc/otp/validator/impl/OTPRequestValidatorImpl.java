package com.tlc.otp.validator.impl;

import com.tlc.otp.model.OTPRequest;
import com.tlc.otp.validator.annotation.OTPRequestValidator;
import com.tlc.validator.util.ValidatorUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * <p>
 * Validates for {@link OTPRequest} details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class OTPRequestValidatorImpl implements ConstraintValidator<OTPRequestValidator, OTPRequest> {

    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^\\+?(?:\\d{1,4}[ \\-]?)?(?:\\(?\\d{1,4}\\)?[ \\-]?)?\\d{1,4}[ \\-]?\\d{1,4}(?:[ \\-]?\\d{1,4})?$");
    private static final short MIN_LENGTH = 5;
    private static final short MAX_LENGTH = 17;

    @Override
    public boolean isValid(final OTPRequest otpRequest, final ConstraintValidatorContext context) {
        final String receiver = otpRequest.getReceiver();

        if (Objects.isNull(receiver) || receiver.isBlank()) {
            return true;
        }

        switch (otpRequest.getChannel()) {
            case EMAIL -> {
                if (!ValidatorUtil.isValidEmail(receiver)) {
                    addConstraintViolationMessage(context, "i18n.validator.error.invalid.email");
                } else {
                    return true;
                }
            }
            case PHONE -> {
                final int length = receiver.replaceAll("\\D", "").length();

                if (length < MIN_LENGTH) {
                    addConstraintViolationMessage(context, "i18n.validator.error.phone.number.min.length");
                } else if (length > MAX_LENGTH) {
                    addConstraintViolationMessage(context, "i18n.validator.error.phone.number.max.length");
                } else if (!PHONE_NUMBER_PATTERN.matcher(receiver).matches()) {
                    addConstraintViolationMessage(context, "i18n.validator.error.invalid.phone.number");
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * <p>
     * Adds the validation error message to the validator context
     * </p>
     *
     * @param validatorContext Represents the {@link ConstraintValidatorContext}
     * @param message          Represents the error message
     */
    private void addConstraintViolationMessage(final ConstraintValidatorContext validatorContext, final String message) {
        validatorContext.disableDefaultConstraintViolation();
        validatorContext.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
