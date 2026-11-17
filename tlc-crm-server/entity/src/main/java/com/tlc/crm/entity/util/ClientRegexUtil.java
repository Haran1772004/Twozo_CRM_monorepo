package com.tlc.crm.entity.util;

/**
 * Utility class for handling various regex patterns and provides a centralized
 * place to store and retrieve regular expressions for different pattern types.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class ClientRegexUtil {

    private static final String URL_REGEX = "^(http|https|ftp):\\/\\/(([^:@]+(:[^:@]*)?@)?[^:\\s]+\\.[A-Za-z]{2,})(:\\d+)?(\\/[^?#]*)?(\\?[^#]*)?(#.*)?$";
    private static final String PHONE_REGEX = "^(?=(?:\\D*\\d){5,17}$)\\+?(?:\\d{1,4}[ \\-]?)?(?:\\(?\\d{1,4}\\)?[ \\-]?)?\\d{1,4}[ \\-]?\\d{1,4}(?:[ \\-]?\\d{1,4})?$";

    private ClientRegexUtil() {
    }

    /**
     * Represents the various regex pattern types supported by this utility.
     *
     * <p>Currently, it includes only the {@code URL} pattern type,
     * but it can be extended in the future to support more pattern types.</p>
     */
    enum PatternType {
        URL,
        PHONE
    }

    /**
     * Gets the regex of a given pattern type.
     *
     * @param regexType Refers the regex type.
     * @return The regex.
     */
    public static String getRegex(final String regexType) {
        return switch (PatternType.valueOf(regexType.toUpperCase())) {
            case URL -> URL_REGEX;
            case PHONE -> PHONE_REGEX;
        };
    }
}
