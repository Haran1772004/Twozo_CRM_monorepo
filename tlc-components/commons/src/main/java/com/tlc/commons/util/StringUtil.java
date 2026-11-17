package com.tlc.commons.util;

import com.tlc.commons.util.constants.NumericalConstant;

import java.util.Collections;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * A Util tool for {@link String} related operations.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public interface StringUtil {

    String EMPTY_STRING = "";
    Set<Character> SPECIAL_CHARS = Set.of('+', '-', '=', '&', '|', '>', '<', '!', '(', ')', '{', '}', '[', ']',
            '^', '"', '~', '*', '?', ':', '\\', '/');

    /**
     * <p>
     * Whether the string is valid.
     * </p>
     *
     * @param string string value.
     * @return whether the string is valid.
     */
    static boolean isValidString(final String string) {
        return !Objects.isNull(string) && !string.isBlank();
    }

    /**
     * <p>
     * Checks whether the passed value has any white space(ex. space, tabs)
     * </p>
     *
     * @param value Represents the value.
     * @return whether the passed value has any white space.
     */
    static boolean containsWhitespace(final String value) {

        if (isValidString(value)) {
            for (final char character : value.toCharArray()) {

                if (Character.isWhitespace(character)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Joins delimiter to the param strings.
     *
     * @param strings String values
     * @return dot appended string.
     */
    static String join(final Delimiter delimiter, final String... strings) {

        if (Objects.isNull(strings) || NumericalConstant.ZERO == strings.length) {
            return null;
        }

        return String.join(delimiter.getDelimiter(), strings);
    }

    /**
     * <p>
     * Joins delimiter to the param strings.
     * </p>
     *
     * @param delimiter Represents the delimiter
     * @param values    Represents the string values
     * @return Optional containing the joined string if values are present; empty Optional otherwise.
     */
    static Optional<String> join(final Delimiter delimiter, final Collection<String> values) {
        if (isNullOrEmpty(values)) {
            return Optional.empty();
        }

        return Optional.of(String.join(delimiter.getDelimiter(), values));
    }

    /**
     * <p>
     * Joins per-term strings with other strings by hyphen.
     * </p>
     * EX- preTerm - "test", strings - ["contact, "company"] OUT -> {"contact-test", "company-test"}
     *
     * @param strings String values
     * @return dot appended string.
     */
    static Collection<String> joinStringsWithHyphen(final String preTerm, final String... strings) {

        if (Objects.isNull(strings) || NumericalConstant.ZERO == strings.length) {
            return Collections.emptyList();
        }
        final Collection<String> stringCollection = new ArrayList<>();

        for (final String string : strings) {
            stringCollection.add(String.join(Delimiter.HYPHEN.getDelimiter(), string, preTerm));
        }

        return stringCollection;
    }

    /**
     * <p>
     * Retrieves string based on the retrieve point's index.
     * </p>
     * EX - string - "testDoc", retrievePoint - "doc" OUT -> test
     *
     * @param retrievePoint Point where retrieve should happen.
     * @param string        String value.
     * @return retrieved value.
     */
    static String subString(final String retrievePoint, final String string) {

        if (isValidString(string)) {
            int from = string.indexOf(retrievePoint);

            return from != -NumericalConstant.ONE ? string.substring(NumericalConstant.ZERO, from) : string;
        }
        return null;
    }

    /**
     * <p>
     * Joins the provided values into a single string
     * </p>
     *
     * @param delimiter Represents the delimiter
     * @param values    Represents the values to be combined
     * @return the combined data
     */
    static Optional<String> joinNonNullValues(final Delimiter delimiter, final Object... values) {
        if (Objects.isNull(values) || 0 == values.length) {
            return Optional.empty();
        }
        return Optional.of(Arrays.stream(values).filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining(delimiter.getDelimiter())));
    }

    /**
     * Joins DOT"." to the param strings.
     *
     * @param strings String values
     * @return dot appended string.
     */
    static String joinStringsWithDot(final String... strings) {
        return join(Delimiter.DOT, strings);
    }

    /**
     * <p>
     * Capitalizes the first letter of the input.
     * </p>
     *
     * @param input Represents input value.
     * @return {@link String}.
     */
    static Optional<String> capitalizeFirstLetter(final String input) {
        return Objects.isNull(input) ? Optional.empty() : Optional.of(Character.toUpperCase(
                input.charAt(NumericalConstant.ZERO)) + input.substring(NumericalConstant.ONE));
    }

    /**
     * \
     * <p>
     * Gets the matched strings in a collection.
     * </p>
     *
     * @param originalStrings    Holds the strings that if matched will be added to the Collection.
     * @param comparativeStrings Holds the strings that's to be compared to the original strings.
     * @return matched strings in a collection.
     */
    static Collection<String> getMatchedCaseSensitiveStrings(final Collection<String> originalStrings,
                                                             final Collection<String> comparativeStrings) {

        if (Objects.nonNull(originalStrings) && Objects.nonNull(comparativeStrings)) {
            return comparativeStrings.stream().filter(originalStrings::contains).toList();
        }

        return Collections.emptyList();
    }

    /**
     * \
     * <p>
     * Gets the matched strings in a collection.
     * </p>
     *
     * @param originalStrings    Holds the strings that if matched will be added to the Collection.
     * @param comparativeStrings Holds the strings that's to be compared to the original strings.
     * @return matched strings in a collection.
     */
    static Collection<String> getMatchedCaseInsensitiveStrings(final Collection<String> originalStrings,
                                                               final Collection<String> comparativeStrings) {

        if (Objects.nonNull(originalStrings) && Objects.nonNull(comparativeStrings)) {
            final Collection<String> result = new ArrayList<>(originalStrings.size());
            final Set<String> lowerCaseStrings = comparativeStrings.stream().map(String::toLowerCase).collect(Collectors.toSet());

            for (final String originalString : originalStrings) {
                if (lowerCaseStrings.contains(originalString.toLowerCase())) {
                    result.add(originalString);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    /**
     * <p>
     * Escapes special characters.
     * </p>
     *
     * @param input String value.
     * @return escaped appended input value.
     */
    static String escapeSpecialCharacters(final String input) {
        final StringBuilder escapedInput = new StringBuilder();

        if (isValidString(input)) {
            final int length = input.length();

            for (int index = NumericalConstant.ZERO; index < length; index++) {
                final char character = input.charAt(index);

                if (SPECIAL_CHARS.contains(character)) {
                    escapedInput.append('\\').append(character);
                } else {
                    escapedInput.append(character);
                }
            }
            return escapedInput.toString();
        }

        return EMPTY_STRING;
    }

    /**
     * <p>
     * Trims the input to specified length.
     * </p>
     *
     * @param input     String value.
     * @param maxLength Length to trim
     * @return trimmed input.
     */
    static String trimInput(final String input, final int maxLength) {

        if (isValidString(input)) {
            return input.length() > maxLength ? input.substring(NumericalConstant.ZERO, maxLength) : input;
        }

        return EMPTY_STRING;
    }

    /**
     * Returns the given number as a zero-padded string based on the specified digit count.
     * <p>
     * If the number contains fewer digits than {@code digitCount}, leading zeroes are added
     * to match the required length. If {@code digitCount} is zero or less, the number is
     * returned without any padding.
     *
     * @param digitCount The total number of digits the result should contain
     * @param number     The numeric value to be formatted
     * @return A string representation of {@code number} padded with leading zeroes,or the number as-is if no padding is required
     */
    static String toZeroPaddedString(final long digitCount, final long number) {
        final StringBuilder builder = new StringBuilder(Long.toString(Math.abs(number)));

        while (builder.length() < digitCount) {
            builder.insert(0, '0');
        }

        if (number < 0) {
            builder.insert(0, '-');
        }

        return builder.toString();
    }

    /**
     * <p>
     * Splits and trims the input string based on the specified delimiter.
     * </p>
     *
     * @param value     the string value to be split
     * @param delimiter the delimiter used for splitting
     * @return the split and trimmed string array values, or an empty array if input is null or blank
     */
    static String[] splitAndTrim(final String value, final Delimiter delimiter) {
        if (Objects.isNull(value)) {
            return new String[0];
        }
        final String[] parts = value.split(delimiter.getDelimiter());

        for (int index = 0; index < parts.length; index++) {
            parts[index] = parts[index].trim();
        }

        return parts;
    }

    /**
     * <p>
     * Splits and normalizes the provided value based on the delimiter
     * </p>
     *
     * @param value      Represents the value to be normalized
     * @param delimiter  Represents the delimiter
     * @return the normalized value
     */
    static Collection<String> splitAndNormalize(final String value, final String delimiter) {
        final Collection<String> parts = new HashSet<>();

        for (final String part : value.split(delimiter)) {

            if (!part.isBlank()) {
                parts.add(part.trim().toLowerCase());
            }
        }

        return parts;
    }

    /**
     * <p>
     * Checks if a collection is null or empty.
     * </p>
     *
     * @param values Represent the values.
     */
    private static <T> boolean isNullOrEmpty(final Collection<T> values) {
        return isNull(values) || values.isEmpty();
    }

    /**
     * <p>
     * Checks if a collection is null or empty.
     * </p>
     *
     * @param value Represent the value.
     */
    private static boolean isNull(final Object value) {
        return Objects.isNull(value);
    }

    /**
     * <p>
     * Joins string values by camel case.
     * </p>
     *
     * @param values String values.
     * @return string value.
     */
    static String joinByCamelCase(final String... values) {

        if (Objects.isNull(values) || NumericalConstant.ZERO == values.length) {
            return "";
        }

        final StringBuilder result = new StringBuilder();

        for (final String value : values) {

            if (Objects.isNull(value) || value.isEmpty()) {
                continue;
            }

            if (result.isEmpty()) {
                result.append(value);
            } else {
                result.append(Character.toUpperCase(value.charAt(NumericalConstant.ZERO))).append(value.substring(NumericalConstant.ONE));
            }
        }
        return result.toString();
    }

    /**
     * <p>
     * Replaces the trailing characters of the given {@link StringBuilder} with the specified replacement string.
     * </p>
     *
     * @param builder      the {@link StringBuilder} whose suffix is to be replaced, must not be {@code null}
     * @param suffixLength the number of trailing characters to replace
     * @param replacement  the string to replace the suffix with
     * @return the modified {@link StringBuilder} with the suffix replaced
     */
    static StringBuilder replaceSuffix(final StringBuilder builder, final int suffixLength, final String replacement) {
        final int length = builder.length();

        return builder.replace(length - suffixLength, length, replacement);
    }

    /**
     * Enum representing various common string delimiters used for joining or splitting strings.
     *
     * @author Sathish
     * @version 1.0
     */
    enum Delimiter {

        UNDER_SCORE("_"),
        HYPHEN("-"),
        COMMA(","),
        COLON(":"),
        FORWARD_SLASH("/"),
        BACK_SLASH("\\"),
        EMPTY_SPACE(" "),
        DOT("."),
        EMPTY_STRING(""),
        PIPELINE("|"),
        SINGLE_QUOTE("'"),
        SINGLE_QUOTES("''"),
        PLUS("+"),
        PLUS_WITH_SPACE(" + "),
        ;

        private final String delimiter;

        Delimiter(final String delimiter) {
            this.delimiter = delimiter;
        }

        public String getDelimiter() {
            return delimiter;
        }
    }
}