package com.tlc.validator.internal;

import com.tlc.validator.type.FutureTime;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Instant;
import java.util.Objects;

/**
 * <p>
 * Validator the given time
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class FutureTimeValidator implements ConstraintValidator<FutureTime, Long> {

    /**
     * <p>
     * Checks whether given phone number is valid or not
     * </p>
     *
     * @param time    The time to validate
     * @param context context in which the constraint is evaluated
     * @return true if the time is valid, otherwise false
     */
    @Override
    public boolean isValid(final Long time, final ConstraintValidatorContext context) {
        return Objects.isNull(time) || time >= Instant.now().toEpochMilli();
    }
}
