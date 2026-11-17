package com.tlc.l10n.model.number;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.l10n.model.country.Country;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Utility class for parsing and validating phone numbers using the {@link Country} enum.
 *
 * <p>This implementation provides manual phone number parsing logic without external dependencies,
 * using the country calling codes defined in the Country enum.</p>
 *
 * @author Sathish
 * @version 1.0
 */
public class PhoneNumberUtil {

    private static final String PLUS_PREFIX = "+";
    private static final Pattern NON_DIGIT = Pattern.compile("[^\\d]");
    private static final int MIN_PHONE_LENGTH = 10;
    private static final int MAX_PHONE_LENGTH = 15;

    /**
     * Maximum digit-length of any country calling code present in the {@link Country} enum.
     * Computed once at class-load time from the actual enum data so it stays correct
     * automatically if new countries are added, without any manual constant update.
     *
     * <p>ITU-T E.164 defines country calling codes as 1–3 digits, which is reflected here.</p>
     */
    private static final int MAX_COUNTRY_CODE_LENGTH = computeMaxCountryCodeLength();

    /**
     * Map of country calling codes (without '+') to Country enum.
     * Built lazily and cached for performance.
     */
    private static volatile Map<String, Country> countryCodeMap;

    /**
     * Parses a raw phone number string and returns a {@link PhoneNumber} wrapper object.
     *
     * <p>Country-code detection is performed <strong>only</strong> when the number is in explicit
     * international format, i.e. it starts with {@code '+'}.  This avoids collision errors where
     * a national number whose digits happen to begin with a valid country calling code prefix
     * (e.g. {@code "9188771234"} ≈ {@code "+91 88771234"}) would be incorrectly identified as
     * an international number.</p>
     *
     * <ul>
     *   <li>{@code "+919788971797"} → country code detected ({@code hasCountryCode() == true})</li>
     *   <li>{@code "9788971797"}    → no {@code '+'} prefix → treated as a national number
     *       ({@code hasCountryCode() == false})</li>
     * </ul>
     *
     * <p>For national numbers with a known country use
     * {@link #parse(String, Country)} or {@link #parseWithFallback(String, Country)}.</p>
     *
     * @param number the raw phone number string to parse (formatting characters are ignored)
     * @return a {@link PhoneNumber} object; {@link PhoneNumber#hasCountryCode()} is {@code false}
     *         when the number does not carry a {@code '+'} prefix
     * @throws ErrorCode if the number is null, empty, or outside the allowed digit-length range
     */
    public static PhoneNumber parse(final String number) {
        validateInput(number);
        final String trimmed = number.trim();

        if (trimmed.startsWith(PLUS_PREFIX)) {
            return parseWithCountryCodeDetection(trimmed);
        }
        return new PhoneNumber(null, extractDigits(trimmed));
    }

    /**
     * Parses a phone number string using a specified country for numbers without country code.
     *
     * <p>This method is useful when parsing national numbers that do not include a country code.
     * For example, parsing "9788971797" with country INDIA will correctly identify it as
     * an Indian phone number.</p>
     *
     * <p>The country code prefix is stripped only when the input is in explicit international
     * format (i.e., it starts with {@code '+'}). This prevents national numbers that happen to
     * begin with the same digits as the country code from being incorrectly truncated.
     * For example, an Indian national number {@code "9188771234"} is preserved as-is, whereas
     * {@code "+919788971797"} will have its {@code "91"} prefix correctly stripped.</p>
     *
     * @param number  the phone number string to parse
     * @param country the {@link Country} to use for parsing
     * @return a {@link PhoneNumber} object containing parsed phone number metadata
     * @throws ErrorCode if the number cannot be parsed
     */
    public static PhoneNumber parse(final String number, final Country country) {
        validateInput(number);
        Objects.requireNonNull(country, "Country cannot be null");

        final String trimmed = number.trim();
        final String countryCode = country.getCode(); // e.g. "+91" — '+' retained

        // Compare the trimmed input directly against the full '+'-prefixed country code
        if (trimmed.startsWith(countryCode)) {
            final String nationalNumber = extractDigits(trimmed.substring(countryCode.length()));

            if (!nationalNumber.isEmpty()) {
                return new PhoneNumber(country, nationalNumber);
            }
        }

        return new PhoneNumber(country, extractDigits(trimmed));
    }

    /**
     * Validates whether the given phone number is a well-formed and usable number.
     *
     * @param number the phone number string to validate (may include country code).
     * @return true if the phone number is valid; false otherwise.
     */
    public static boolean isValid(final String number) {
        if (Objects.isNull(number) || number.trim().isEmpty()) {
            return false;
        }

        try {
            final PhoneNumber phoneNumber = parse(number);
            final String nationalNumber = phoneNumber.hasCountryCode()
                    ? phoneNumber.getNumber()
                    : extractDigits(number);
            final int length = nationalNumber.length();

            return length >= MIN_PHONE_LENGTH && length <= MAX_PHONE_LENGTH;
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * Validates whether the given phone number is a well-formed and usable number
     * using a specified country for numbers without country code.
     *
     * @param number  the phone number string to validate
     * @param country the {@link Country} to use for validation
     * @return true if the phone number is valid; false otherwise.
     */
    public static boolean isValid(final String number, final Country country) {
        if (Objects.isNull(number) || number.trim().isEmpty()) {
            return false;
        }

        try {
            final String digitsOnly = extractDigits(number);
            final int length = digitsOnly.length();

            if (length < MIN_PHONE_LENGTH || length > MAX_PHONE_LENGTH) {
                return false;
            }

            parse(number, country);

            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * Attempts to parse a phone number with automatic country code detection.
     * If detection fails, falls back to the specified default country.
     *
     * <p>This is useful when you have numbers that may or may not include a country code prefix.</p>
     *
     * <p>Example:
     * <ul>
     *   <li>"+919788971797" → Parsed with detected country (India) via auto-detection</li>
     *   <li>"9788971797" → Auto-detection fails (no detectable country code prefix),
     *       falls back to defaultCountry (e.g., INDIA)</li>
     * </ul>
     * </p>
     *
     * <p><strong>Note:</strong> Numbers whose digits happen to begin with a valid country
     * calling code (e.g., "9673487278" starts with "967" = Yemen) will be auto-detected as
     * that country and the fallback will <em>not</em> be triggered. Use
     * {@link #parse(String, Country)} directly if auto-detection must be suppressed.</p>
     *
     * @param number         the phone number string to parse
     * @param defaultCountry the country to use if auto-detection fails
     * @return a {@link PhoneNumber} object containing parsed phone number metadata
     */
    public static PhoneNumber parseWithFallback(final String number, final Country defaultCountry) {
        Objects.requireNonNull(defaultCountry, "Default country cannot be null");

        try {
            return parse(number);
        } catch (Exception exception) {
            return parse(number, defaultCountry);
        }
    }

    /**
     * Checks if the given number appears to be a national number without a country code prefix.
     *
     * <p>A number is considered national when it does <em>not</em> start with {@code '+'}.
     * Only an explicit {@code '+'} prefix is treated as an unambiguous signal that a country
     * calling code is present — digit-only heuristics are deliberately avoided to prevent
     * collision errors (see {@link #parse(String)}).</p>
     *
     * @param number the phone number string to check
     * @return {@code true} if the number has no {@code '+'} prefix (national format),
     *         {@code false} if it starts with {@code '+'}
     */
    public static boolean isLikelyNationalNumber(final String number) {
        if (Objects.isNull(number) || number.trim().isEmpty()) {
            return false;
        }
        return !number.trim().startsWith(PLUS_PREFIX);
    }

    /**
     * Extracts only digits from the input string.
     *
     * @param number the input phone number
     * @return string containing only digits
     */
    private static String extractDigits(final String number) {
        if (Objects.isNull(number)) {
            return "";
        }

        return NON_DIGIT.matcher(number).replaceAll("");
    }

    /**
     * Validates the input phone number string.
     *
     * @param number the phone number to validate
     * @throws ErrorCode if the input is invalid
     */
    private static void validateInput(final String number) {
        if (Objects.isNull(number) || number.trim().isEmpty()) {
            throw ErrorCode.get(ErrorCodes.PARSE_FAILED, "Phone number cannot be null or empty");
        }

        final String digitsOnly = extractDigits(number);

        if (digitsOnly.isEmpty()) {
            throw ErrorCode.get(ErrorCodes.PARSE_FAILED, "Phone number must contain digits");
        }

        if (digitsOnly.length() < MIN_PHONE_LENGTH) {
            throw ErrorCode.get(ErrorCodes.PARSE_FAILED, "Phone number is too short");
        }

        if (digitsOnly.length() > MAX_PHONE_LENGTH) {
            throw ErrorCode.get(ErrorCodes.PARSE_FAILED, "Phone number is too long");
        }
    }

    /**
     * Parses a '+'-prefixed string by detecting the country code using longest-to-shortest
     * prefix matching against map keys that include the {@code '+'} character.
     *
     * @param trimmed the trimmed phone number string that starts with {@code '+'}
     * @return a {@link PhoneNumber} with the detected country and national number
     * @throws ErrorCode if no known country calling code prefix can be matched
     */
    private static PhoneNumber parseWithCountryCodeDetection(final String trimmed) {
        final Map<String, Country> codeMap = getCountryCodeMap();

        for (int length = MAX_COUNTRY_CODE_LENGTH; length >= 2; length--) {
            if (length > trimmed.length()) {
                continue;
            }
            final String potentialCode = trimmed.substring(0, length);
            final Country country = codeMap.get(potentialCode);

            if (Objects.nonNull(country)) {
                final String nationalNumber = extractDigits(trimmed.substring(length));

                if (!nationalNumber.isEmpty()) {
                    return new PhoneNumber(country, nationalNumber);
                }
            }
        }

        throw ErrorCode.get(ErrorCodes.PARSE_FAILED, "Unable to detect country code from the given number");
    }

    /**
     * Returns the cached map of country calling codes to Country enum.
     * Uses double-checked locking for thread-safe lazy initialization.
     *
     * @return map of country codes to Country
     */
    private static Map<String, Country> getCountryCodeMap() {
        if (countryCodeMap == null) {
            synchronized (PhoneNumberUtil.class) {
                if (countryCodeMap == null) {
                    countryCodeMap = buildCountryCodeMap();
                }
            }
        }

        return countryCodeMap;
    }

    /**
     * Builds a map of country calling codes (without '+') to Country enum values.
     * For countries sharing the same code (e.g., +1 for US, Canada), uses the first match.
     *
     * @return map of country codes to Country
     */
    private static Map<String, Country> buildCountryCodeMap() {
        final Map<String, Country> map = new HashMap<>();

        for (final Country country : Country.values()) {
            map.putIfAbsent(country.getCode(), country);
        }

        return map;
    }

    /**
     * Computes the maximum character-length (including the leading {@code '+'}) of all country
     * calling codes in the {@link Country} enum.
     *
     * <p>Because the map keys and comparisons now include {@code '+'}, this value is used
     * directly as the upper bound for the prefix-matching loop in
     * {@link #parseWithCountryCodeDetection(String)}.</p>
     *
     * @return the length of the longest country calling code (with {@code '+'}) in
     *         {@link Country#values()} — e.g. {@code 4} for {@code "+353"}
     */
    private static int computeMaxCountryCodeLength() {
        int max = 0;

        for (final Country country : Country.values()) {
            final int codeLength = country.getCode().length();

            if (codeLength > max) {
                max = codeLength;
            }
        }

        return max;
    }
}
