package com.tlc.validator.internal;

import com.tlc.validator.type.URL;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * <p>
 * It validates the URL
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class URLValidator implements ConstraintValidator<URL, String> {

    /**
     * <p>
     * Checks whether a given url represents a valid URL or not
     * </p>
     *
     * @param url     The URL string to validate
     * @param context The validation context
     * @return true if the URL string is valid according to the regex pattern, false otherwise
     */
    @Override
    public boolean isValid(final String url, final ConstraintValidatorContext context) {

        if (Objects.nonNull(url)) {
            final String websiteRegex = "^(http|https|ftp):\\/\\/(([^:@]+(:[^:@]*)?@)?[^:\\s]+\\.[A-Za-z]{2,})(:\\d+)?(\\/[^?#]*)?(\\?[^#]*)?(#.*)?$";

            return Pattern.compile(websiteRegex).matcher(url).matches();
        }
        return true;
    }
}
