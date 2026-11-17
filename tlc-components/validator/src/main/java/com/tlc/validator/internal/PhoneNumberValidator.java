package com.tlc.validator.internal;

import com.tlc.validator.type.Phone;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * <p>
 * Validator that validates phone numbers
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class PhoneNumberValidator implements ConstraintValidator<Phone, String> {
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^\\+?(?:\\d{1,4}[ \\-]?)?(?:\\(?\\d{1,4}\\)?[ \\-]?)?\\d{1,4}[ \\-]?\\d{1,4}(?:[ \\-]?\\d{1,4})?$");
    private static final short MIN_LENGTH = 5;
    private static final short MAX_LENGTH = 17;

    /**
     * <p>
     * Checks whether given phone number is valid or not
     * </p>
     * Some possible valid formats:
     *
     * <ul>
     *   <li>12345</li>
     *   <li>+1234-5678</li>
     *   <li>1234-5678</li>
     *   <li>12-34-567</li>
     *   <li>12-34-56-78</li>
     *   <li>1234 567 8901</li>
     *   <li>1234-567-8901</li>
     *   <li>+1234 5678</li>
     *   <li>+91-9876567898</li>
     *   <li>+1 (202) 555-0143</li>
     *   <li>+1234 567 8901</li>
     *   <li>+1234-567-8901</li>
     *   <li>123 (4567) 8901</li>
     *   <li>123-(4567)-8901</li>
     *   <li>(123) 4567 8901</li>
     *   <li>(123)-4567-8901</li>
     *   <li>+12 (345) 678 901</li>
     *   <li>+12-345-678-901</li>
     *   <li>+12 (3456) 7890</li>
     *   <li>1234 (5678) 9012</li>
     *   <li>(123) 456-7890</li>
     *   <li>(1234) 567-890</li>
     *   <li>(12) 345 6789</li>
     *   <li>+1-(1)-1-1-3</li>
     *   <li>+12-(12)-12-12-23</li>
     *   <li>+122-(122)-122-122-232</li>
     *   <li>+1224-(1224)-122-122-232</li>
     *   <li>+122-(122)-1224-122-2323</li>
     *   <li>+122-(122)-12-12-22</li>
     *   <li>+1234-(1)-1234-1234</li>
     *   <li>+1234-(12)-1234-1234</li>
     *   <li>+1234-(123)-1234-1234</li>
     *   <li>+1234-(1234)-1234-1234</li>
     *   <li>+1234-(1234)-1234-12-34</li>
     *   <li>+1234 (1) 1234 1234</li>
     *   <li>+1234 (12) 1234 1234</li>
     *   <li>+1234 (123) 1234 1234</li>
     *   <li>+1234 (1234) 1234 1234</li>
     *   <li>+1234 (1234) 1234 12 34</li>
     *   <li>+12345678912345678</li>
     *   <li>+12345678-912345678</li>
     *   <li>+1234(5678)-912345678</li>
     *   <li>12345678912345678</li>
     * </ul>
     *
     * @param value   The phone number to validate
     * @param context context in which the constraint is evaluated
     * @return true if the phone number is valid, otherwise false
     */
    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (Objects.isNull(value) || value.isEmpty()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        final int length = value.replaceAll("\\D", "").length();

        if (length < MIN_LENGTH) {
            context.buildConstraintViolationWithTemplate("i18n.validator.error.phone.number.min.length").addConstraintViolation();
        } else if (length > MAX_LENGTH) {
            context.buildConstraintViolationWithTemplate("i18n.validator.error.phone.number.max.length").addConstraintViolation();
        } else if (!PHONE_NUMBER_PATTERN.matcher(value).matches()) {
            context.buildConstraintViolationWithTemplate("i18n.validator.error.invalid.phone.number").addConstraintViolation();
        } else {
            return true;
        }

        return false;
    }
}
