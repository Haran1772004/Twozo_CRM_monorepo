package com.tlc.commons.json.resolver;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.json.resolver.model.PlaceholderData;
import com.tlc.commons.json.resolver.model.PlaceholderEntries;
import com.tlc.commons.json.resolver.model.PlaceholderEntry;
import com.tlc.commons.json.resolver.model.PlaceholderKey;
import com.tlc.commons.json.resolver.model.PlaceholderValue;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * A generic, production-ready utility to resolve {@code {{KEY}}} placeholders in any JSON structure
 * using a {@link PlaceholderData} POJO that holds both scalar and array replacement values.
 * </p>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>Placeholder format: {@code {{KEY}}}, {@code {{KEY}}:defaultValue}, or {@code {{KEY}}:} (empty default)</li>
 *   <li>Inline scalar placeholder format: {@code $$${{KEY}}:defaultValue$$$} — embeds a resolved scalar
 *       inside any larger string (e.g., {@code "Show $$${{SOURCE_COMPANY}}:Company$$$ Name"})</li>
 *   <li>Accepts {@link PlaceholderData} POJO for type-safe, categorized replacements</li>
 *   <li>Handles arbitrarily nested {@link JsonObject} and {@link JsonArray} structures recursively</li>
 *   <li>Supports array-of-values expansion via the {@code "hasArrayOfValues": true} flag</li>
 *   <li>Falls back to the default value when no replacement is found for a placeholder</li>
 *   <li>Leaves unresolved placeholders unchanged when no mapping or default value is found</li>
 *   <li>Null-safe: gracefully handles null inputs at every level</li>
 *   <li>Optimized: pre-compiled regex pattern; string values without placeholders are skipped early</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * PlaceholderData data = PlaceholderData.builder()
 *         .scalar("TITLE", "My Document")
 *         .addArrayRow("ITEMS", Map.of("ITEM_NAME", "Alice", "ITEM_EMAIL", "alice@example.com"))
 *         .addArrayRow("ITEMS", Map.of("ITEM_NAME", "Bob", "ITEM_EMAIL", "bob@example.com"))
 *         .build();
 *
 * String resolved = JsonPlaceholderResolver.resolve(templateJsonString, data);
 * }</pre>
 *
 * @author Sathish
 * @version 2.0
 */
public final class JsonPlaceholderResolver {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");
    private static final Pattern PLACEHOLDER_WITH_DEFAULT_PATTERN = Pattern.compile("^\\{\\{([^}]+)}}(?::(.*))?$");

    /**
     * Pattern that matches an inline scalar placeholder of the form {@code $$${{KEY}}$$$} or
     * {@code $$${{KEY}}:defaultValue$$$}.
     * <ul>
     *   <li>Group 1 — the placeholder key (e.g., {@code SOURCE_COMPANY})</li>
     *   <li>Group 2 — the optional default value string; {@code null} when the {@code :} suffix is absent</li>
     * </ul>
     */
    private static final Pattern INLINE_SCALAR_PATTERN =
            Pattern.compile("\\$\\$\\$\\{\\{([^}]+)}}(?::(.*?))?\\$\\$\\$");

    private static final String INLINE_PLACEHOLDER_DELIMITER = "$$$";
    private static final String HAS_ARRAY_OF_VALUES = "hasArrayOfValues";
    private static final String VALUES = "values";
    private static final String PLACEHOLDER_FIELD = "placeHolder";
    private static final String PLACEHOLDER_FORMAT = "{{%s}}";
    private static final String PLACEHOLDER_STARTING_SYMBOL = "{{";
    private static final char DEFAULT_VALUE_SEPARATOR = ':';

    private JsonPlaceholderResolver() {
    }

    /**
     * <p>
     * Resolves all {@code {{KEY}}} placeholders in the given JSON string using the supplied
     * {@link PlaceholderData}.
     * </p>
     *
     * @param jsonString      The JSON string that may contain placeholders.
     * @param placeholderData The POJO holding scalar and array replacement values.
     * @return A new JSON string with resolved placeholders, or the original string if inputs are null/empty.
     */
    public static String resolve(final String jsonString, final PlaceholderData placeholderData) {
        if (Objects.isNull(jsonString) || jsonString.isEmpty()) {
            return jsonString;
        }

        if (Objects.isNull(placeholderData)) {
            return jsonString;
        }
        final JsonObject root = Json.object(jsonString);

        resolveJsonObject(root, placeholderData);

        return root.toString();
    }

    /**
     * <p>
     * Resolves all {@code {{KEY}}} placeholders in the given {@link JsonObject} <b>in place</b>.
     * </p>
     *
     * @param jsonObject      The JSON object whose string values may contain placeholders.
     * @param placeholderData The POJO holding scalar and array replacement values.
     * @return The same {@link JsonObject} instance (mutated) for fluent chaining.
     */
    public static JsonObject resolve(final JsonObject jsonObject, final PlaceholderData placeholderData) {
        if (Objects.isNull(jsonObject) || Objects.isNull(placeholderData)) {
            return jsonObject;
        }

        resolveJsonObject(jsonObject, placeholderData);
        return jsonObject;
    }

    /**
     * <p>
     * Recursively walks through the {@link JsonObject} and resolves placeholders in all string values.
     * When a child node has {@code hasArrayOfValues == true}, the template entry inside its
     * {@code values} array is expanded before scalar resolution continues.
     * </p>
     *
     * @param jsonObject      The current JSON object node.
     * @param placeholderData The POJO holding scalar and array replacement values.
     */
    private static void resolveJsonObject(final JsonObject jsonObject, final PlaceholderData placeholderData) {
        final Map<String, Object> pendingPuts = new LinkedHashMap<>();

        jsonObject.forEach((key, value) -> {
            if (value instanceof String stringValue) {
                final Object resolved = resolveValue(stringValue, placeholderData);

                if (!Objects.equals(resolved, stringValue)) {
                    pendingPuts.put(key, resolved);
                }
            } else if (value instanceof Map) {
                final JsonObject child = jsonObject.optJsonObject(key);

                if (Objects.nonNull(child)) {
                    expandArrayValues(child, placeholderData);
                    resolveJsonObject(child, placeholderData);
                }
            } else if (value instanceof Iterable) {
                final JsonArray child = jsonObject.optJsonArray(key);

                if (Objects.nonNull(child)) {
                    resolveJsonArray(child, placeholderData);
                }
            }
        });

        pendingPuts.forEach(jsonObject::put);
    }

    /**
     * <p>
     * Recursively walks through the {@link JsonArray} and resolves placeholders in all elements.
     * </p>
     *
     * @param jsonArray       The current JSON array node.
     * @param placeholderData The POJO holding scalar and array replacement values.
     */
    private static void resolveJsonArray(final JsonArray jsonArray, final PlaceholderData placeholderData) {
        final Map<Integer, Object> pendingPuts = new TreeMap<>();
        final Collection<String> pendingRemovables = new HashSet<>();
        final int size = jsonArray.size();

        for (int index = 0; index < size; index++) {
            final Object element = jsonArray.get(index);

            if (element instanceof Map) {
                final JsonObject child = jsonArray.getJsonObject(index);

                if (Objects.nonNull(child)) {
                    expandArrayValues(child, placeholderData);
                    resolveJsonObject(child, placeholderData);
                }
            } else if (element instanceof Iterable) {
                final JsonArray child = jsonArray.optJsonArray(index);

                if (Objects.nonNull(child)) {
                    resolveJsonArray(child, placeholderData);
                }
            } else if (element instanceof String stringValue) {
                final Object resolved = resolveValue(stringValue, placeholderData);

                if (!Objects.equals(resolved, stringValue)) {
                    pendingRemovables.add(stringValue);
                    pendingPuts.put(index, resolved);
                }
            }
        }
        pendingRemovables.forEach(jsonArray::remove);
        pendingPuts.values().forEach(jsonArray::put);
    }

    /**
     * <p>
     * Inspects a {@link JsonObject} node for the {@code "hasArrayOfValues": true} flag.
     * When present, the method:
     * </p>
     * <ol>
     *   <li>Reads the placeholder key from the {@code "placeHolder"} field.</li>
     *   <li>Looks up the corresponding row list from {@link PlaceholderData}.</li>
     *   <li>Takes the first element in the existing {@code "values"} array as the template.</li>
     *   <li>Clones the template once per row, substituting each placeholder with the row value.</li>
     *   <li>Replaces the original {@code "values"} array with the fully expanded array.</li>
     * </ol>
     *
     * @param jsonObject      The JSON object to inspect and potentially expand.
     * @param placeholderData The POJO holding scalar and array replacement values.
     */
    private static void expandArrayValues(final JsonObject jsonObject, final PlaceholderData placeholderData) {
        if (!jsonObject.optBoolean(HAS_ARRAY_OF_VALUES)) {
            return;
        }
        final String rawPlaceholder = jsonObject.optString(PLACEHOLDER_FIELD, null);

        if (Objects.isNull(rawPlaceholder)) {
            return;
        }

        final String placeholderKey = extractPlaceholderKey(rawPlaceholder);

        if (Objects.isNull(placeholderKey)) {
            return;
        }
        final PlaceholderKey.Provider placeHolderKeyProvider = placeholderData.getPlaceHolderKeyProvider();
        final List<PlaceholderEntries> dataRows = placeholderData.getArrayRows(placeHolderKeyProvider.getKey(placeholderKey));

        if (Objects.isNull(dataRows) || dataRows.isEmpty()) {
            return;
        }

        final JsonArray templateValues = jsonObject.optJsonArray(VALUES);

        if (Objects.isNull(templateValues) || templateValues.size() == 0) {
            return;
        }

        final JsonObject template = templateValues.optJsonObject(0);

        if (Objects.isNull(template)) {
            return;
        }

        final Set<String> templateKeys = template.keySet();
        final Map<String, String> fieldPlaceholderMap = new LinkedHashMap<>();

        for (final String fieldKey : templateKeys) {
            final Object templateVal = template.opt(fieldKey);

            if (templateVal instanceof String templateStr) {
                final String phKey = extractPlaceholderKey(templateStr);

                if (Objects.nonNull(phKey)) {
                    fieldPlaceholderMap.put(fieldKey, phKey);
                }
            }
        }

        final JsonArray expandedValues = Json.array();

        for (final PlaceholderEntries row : dataRows) {
            final JsonObject expandedRow = Json.object(template.toString());

            for (final Map.Entry<String, String> mapping : fieldPlaceholderMap.entrySet()) {
                final String fieldKey = mapping.getKey();
                final String placeHolderKey = mapping.getValue();
                final Object currentVal = expandedRow.opt(fieldKey);

                if (!(currentVal instanceof String currentStr)) {
                    continue;
                }

                final String fullPlaceholder = String.format(PLACEHOLDER_FORMAT, placeHolderKey);
                final int placeholderStart = currentStr.indexOf(fullPlaceholder);

                if (placeholderStart < 0) {
                    continue;
                }

                final int afterPlaceholder = placeholderStart + fullPlaceholder.length();
                final boolean hasDefaultSuffix = afterPlaceholder < currentStr.length()
                        && currentStr.charAt(afterPlaceholder) == DEFAULT_VALUE_SEPARATOR;
                final PlaceholderEntry entry = row.getEntry(placeHolderKeyProvider.getKey(placeHolderKey));

                if (Objects.nonNull(entry)) {
                    final boolean isExactMatch = placeholderStart == 0
                            && (afterPlaceholder == currentStr.length()
                            || (hasDefaultSuffix && afterPlaceholder + 1 >= currentStr.length()));

                    if (isExactMatch || (hasDefaultSuffix && placeholderStart == 0)) {
                        final Object nativeValue = toJsonCompatibleValue(entry.getPlaceholderValue());

                        if (Objects.nonNull(nativeValue)) {
                            expandedRow.put(fieldKey, nativeValue);
                        } else {
                            final String defaultValue = hasDefaultSuffix
                                    ? currentStr.substring(afterPlaceholder + 1) : null;
                            expandedRow.put(fieldKey, Objects.nonNull(defaultValue) ? defaultValue : currentStr);
                        }
                    } else {
                        final String stringReplacement = toStringValue(entry.getPlaceholderValue());

                        if (Objects.nonNull(stringReplacement)) {
                            final String tokenToReplace = hasDefaultSuffix
                                    ? currentStr.substring(placeholderStart) : fullPlaceholder;
                            expandedRow.put(fieldKey, currentStr.replace(tokenToReplace, stringReplacement));
                        }
                    }
                } else {
                    final String defaultValue = hasDefaultSuffix
                            ? currentStr.substring(afterPlaceholder + 1) : null;

                    if (Objects.nonNull(defaultValue)) {
                        expandedRow.put(fieldKey, defaultValue);
                    }
                }
            }

            expandedValues.put(expandedRow);
        }

        jsonObject.put(VALUES, expandedValues);
    }

    /**
     * <p>
     * Replaces all occurrences of {@code {{KEY}}} or {@code {{KEY}}:defaultValue} in the given
     * string with scalar values from the {@link PlaceholderData}. When a replacement is found,
     * the placeholder and its optional default value suffix are replaced. When no replacement
     * is found and a default value is present, the default value is used. If neither is available,
     * the placeholder is left unchanged.
     * </p>
     *
     * @param text            The string potentially containing placeholders.
     * @param placeholderData The POJO holding scalar replacement values.
     * @return The resolved string.
     */
    private static String resolvePlaceholders(final String text, final PlaceholderData placeholderData) {
        if (Objects.isNull(text) || !text.contains(PLACEHOLDER_STARTING_SYMBOL)) {
            return text;
        }

        final Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        final StringBuilder result = new StringBuilder();
        final PlaceholderKey.Provider placeHolderKeyProvider = placeholderData.getPlaceHolderKeyProvider();
        int lastEnd = 0;

        while (matcher.find()) {
            final String key = matcher.group(1);
            final PlaceholderEntry entry = placeholderData.getScalarEntry(placeHolderKeyProvider.getKey(key));

            result.append(text, lastEnd, matcher.start());

            if (Objects.nonNull(entry)) {
                final String stringValue = toStringValue(entry.getPlaceholderValue());

                if (Objects.nonNull(stringValue)) {
                    result.append(stringValue);
                    lastEnd = skipDefaultValueSuffix(text, matcher.end());
                } else {
                    final PlaceholderValue defaultPlaceholderValue = extractDefaultValue(text, matcher.end());

                    if (defaultPlaceholderValue.isNull()) {
                        result.append(matcher.group(0));
                        lastEnd = matcher.end();
                    } else {
                        result.append(defaultPlaceholderValue.getRawValue());
                        lastEnd = skipDefaultValueSuffix(text, matcher.end());
                    }
                }
            } else {
                final PlaceholderValue defaultPlaceholderValue = extractDefaultValue(text, matcher.end());

                if (defaultPlaceholderValue.isNull()) {
                    result.append(matcher.group(0));
                    lastEnd = matcher.end();
                } else {
                    result.append(defaultPlaceholderValue.getRawValue());
                    lastEnd = skipDefaultValueSuffix(text, matcher.end());
                }
            }
        }

        result.append(text, lastEnd, text.length());

        return result.toString();
    }

    /**
     * <p>
     * Resolves all <em>inline scalar placeholders</em> of the form
     * {@code $$${{KEY}}$$$} or {@code $$${{KEY}}:defaultValue$$$} that are embedded within
     * a larger string. Each matching token is replaced with:
     * </p>
     * <ol>
     *   <li>The scalar value from {@link PlaceholderData} when an entry for {@code KEY} exists.</li>
     *   <li>The {@code defaultValue} when no entry is found and a default is provided.</li>
     *   <li>The original {@code $$$…$$$} token unchanged when neither a value nor a default is available.</li>
     * </ol>
     * <p>
     * Unlike standard placeholders, the resolved replacement is always treated as a plain string
     * (the surrounding context is a larger string, so native-type promotion does not apply here).
     * </p>
     *
     * <h4>Examples</h4>
     * <pre>{@code
     * // SOURCE_COMPANY → "Acme Corp"
     * "Show $$${{SOURCE_COMPANY}}:Company$$$ Name"  →  "Show Acme Corp Name"
     *
     * // SOURCE_COMPANY not present, default used
     * "Show $$${{SOURCE_COMPANY}}:Company$$$ Name"  →  "Show Company Name"
     *
     * // Nested as default value of a standard placeholder
     * "{{LABEL}}:$$${{SOURCE_CONTACT}}:Contacts$$$"  →  "{{LABEL}}:Contacts"
     * }</pre>
     *
     * @param text            The string potentially containing inline scalar placeholders.
     * @param placeholderData The POJO holding scalar replacement values.
     * @return The string with all inline scalar placeholders resolved.
     */
    private static String resolveInlineScalarPlaceholders(final String text, final PlaceholderData placeholderData) {
        if (Objects.isNull(text) || !text.contains(INLINE_PLACEHOLDER_DELIMITER)) {
            return text;
        }

        final Matcher matcher = INLINE_SCALAR_PATTERN.matcher(text);

        if (!matcher.find()) {
            return text;
        }

        final StringBuilder result = new StringBuilder();
        final PlaceholderKey.Provider keyProvider = placeholderData.getPlaceHolderKeyProvider();

        matcher.reset();

        while (matcher.find()) {
            final String key = matcher.group(1);
            final String defaultValue = matcher.group(2); // null when no ':' suffix; empty when "{{KEY}}:$$$"

            final PlaceholderEntry entry = placeholderData.getScalarEntry(keyProvider.getKey(key));
            final String replacement;

            if (Objects.nonNull(entry)) {
                final String resolvedValue = toStringValue(entry.getPlaceholderValue());

                replacement = Objects.isNull(resolvedValue) ? Objects.isNull(defaultValue) ? matcher.group(0) : defaultValue : resolvedValue;
            } else {
                replacement = Objects.isNull(defaultValue) ? matcher.group(0) : defaultValue;
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * <p>
     * Resolves a string value that may contain placeholders. Processing order:
     * </p>
     * <ol>
     *   <li><b>Inline scalar placeholders</b> ({@code $$${{KEY}}:defaultValue$$$}) are resolved first,
     *       substituting the entire {@code $$$…$$$} token with the scalar value (or default). This step
     *       also handles the case where an inline scalar appears as the default value of a standard
     *       placeholder, e.g. {@code {{LABEL}}:$$${{SOURCE_CONTACT}}:Contacts$$$}.</li>
     *   <li><b>Standard placeholders</b> ({@code {{KEY}}} / {@code {{KEY}}:default}) are then resolved
     *       on the pre-processed string using the existing logic.</li>
     * </ol>
     * <p>
     * If the entire string (after inline resolution) matches {@code {{KEY}}:default}, the resolved value
     * is returned in its native JSON type (String, Boolean, Number, JsonObject, JsonArray). Mixed-content
     * strings are resolved using {@link #resolvePlaceholders}.
     * </p>
     *
     * @param text            The string potentially containing placeholders.
     * @param placeholderData The POJO holding scalar replacement values.
     * @return The resolved value in its native type, the default value, or the original string.
     */
    private static Object resolveValue(final String text, final PlaceholderData placeholderData) {
        if (Objects.isNull(text)) {
            return null;
        }
        final String processedText = text.contains(INLINE_PLACEHOLDER_DELIMITER)
                ? resolveInlineScalarPlaceholders(text, placeholderData)
                : text;

        if (processedText.contains(PLACEHOLDER_STARTING_SYMBOL)) {
            final PlaceholderKey.Provider placeHolderKeyProvider = placeholderData.getPlaceHolderKeyProvider();
            final Matcher defaultMatcher = PLACEHOLDER_WITH_DEFAULT_PATTERN.matcher(processedText);

            if (defaultMatcher.matches()) {
                final String key = defaultMatcher.group(1);
                final String defaultValueStr = defaultMatcher.group(2);
                final PlaceholderEntry entry = placeholderData.getScalarEntry(placeHolderKeyProvider.getKey(key));

                if (Objects.nonNull(entry)) {
                    final Object resolved = toJsonCompatibleValue(entry.getPlaceholderValue());

                    if (Objects.nonNull(resolved)) {
                        return resolved;
                    }
                }

                if (Objects.nonNull(defaultValueStr)) {
                    final PlaceholderValue parsedDefault = parseDefaultValue(defaultValueStr);

                    return parsedDefault.isNull() ? processedText : parsedDefault.getRawValue();
                }

                return processedText;
            }

            return resolvePlaceholders(processedText, placeholderData);
        }
        return processedText;
    }

    /**
     * <p>
     * Converts a {@link PlaceholderValue} to a JSON-compatible value suitable for direct
     * insertion into a {@link JsonObject}. Returns the native type (String, Boolean, Number,
     * JsonObject, JsonArray) or {@code null}.
     * </p>
     *
     * @param placeholderValue The placeholder value to convert.
     * @return The JSON-compatible value, or {@code null} if the value is null.
     */
    private static Object toJsonCompatibleValue(final PlaceholderValue placeholderValue) {
        if (Objects.isNull(placeholderValue) || placeholderValue.isNull()) {
            return null;
        }

        return placeholderValue.getRawValue();
    }

    /**
     * <p>
     * Converts a {@link PlaceholderValue} to its string representation for use in
     * partial placeholder substitution within mixed-content strings.
     * </p>
     *
     * @param placeholderValue The placeholder value to convert.
     * @return The string representation, or {@code null} if the value is null.
     */
    private static String toStringValue(final PlaceholderValue placeholderValue) {
        if (Objects.isNull(placeholderValue) || placeholderValue.isNull()) {
            return null;
        }

        final Object rawValue = placeholderValue.getRawValue();

        return Objects.isNull(rawValue) ? null : String.valueOf(rawValue);
    }

    /**
     * <p>
     * Extracts the default value from the text at the given position. If the character at the position
     * is the default value separator ({@code ':'}), the substring after it is returned as the default value.
     * When the separator is present but nothing follows (e.g., {@code {{KEY}}:}), an empty string is returned.
     * </p>
     *
     * @param text     The full text containing the placeholder and optional default value.
     * @param position The position immediately after the placeholder's closing braces.
     * @return The default value string (may be empty), or {@code null} if no default value separator is present.
     */
    private static PlaceholderValue extractDefaultValue(final String text, final int position) {
        if (position < text.length() && text.charAt(position) == DEFAULT_VALUE_SEPARATOR) {
            final String defaultValue = text.substring(position + 1);

            return parseDefaultValue(defaultValue);
        }

        return PlaceholderValue.ofNull();
    }

    /**
     * <p>
     * Parses a raw default value string into a typed {@link PlaceholderValue}.
     * The string is tested in the following order:
     * </p>
     * <ol>
     *   <li>Empty string — returned as {@link PlaceholderValue#of(String)} with an empty string</li>
     *   <li>{@code "true"} or {@code "false"} (case-insensitive) — returned as {@link PlaceholderValue#of(Boolean)}</li>
     *   <li>Valid integer (within {@link Long} range) — returned as {@link PlaceholderValue#of(Number)}</li>
     *   <li>Valid decimal — returned as {@link PlaceholderValue#of(Number)}</li>
     *   <li>Everything else — returned as {@link PlaceholderValue#of(String)}</li>
     * </ol>
     *
     * @param defaultValue The raw default value string to parse.
     * @return A typed {@link PlaceholderValue} representing the parsed value.
     */
    private static PlaceholderValue parseDefaultValue(final String defaultValue) {
        if (defaultValue.isEmpty()) {
            return PlaceholderValue.of(defaultValue);
        }

        if ("true".equalsIgnoreCase(defaultValue) || "false".equalsIgnoreCase(defaultValue)) {
            return PlaceholderValue.of(Boolean.parseBoolean(defaultValue));
        }

        try {
            final long longValue = Long.parseLong(defaultValue);

            return PlaceholderValue.of(longValue);
        } catch (final NumberFormatException ignored) {
        }

        try {
            final double doubleValue = Double.parseDouble(defaultValue);

            if (!Double.isInfinite(doubleValue) && !Double.isNaN(doubleValue)) {
                return PlaceholderValue.of(doubleValue);
            }
        } catch (final NumberFormatException ignored) {
        }

        return PlaceholderValue.of(defaultValue);
    }

    /**
     * <p>
     * Returns the index past the default value suffix if one is present at the given position.
     * Used to advance the scan position in the source text when the default value portion
     * should be consumed (i.e., when a replacement value was applied).
     * </p>
     *
     * @param text     The full text containing the placeholder and optional default value.
     * @param position The position immediately after the placeholder's closing braces.
     * @return The index past the default value suffix, or the original position if no suffix is present.
     */
    private static int skipDefaultValueSuffix(final String text, final int position) {
        if (position < text.length() && text.charAt(position) == DEFAULT_VALUE_SEPARATOR) {
            return text.length();
        }

        return position;
    }


    /**
     * <p>
     * Extracts the placeholder key from a string of the form {@code {{KEY}}} or {@code {{KEY}}:default}.
     * Returns {@code null} if no placeholder is found.
     * </p>
     *
     * @param value The string to extract from.
     * @return The placeholder key, or {@code null}.
     */
    private static String extractPlaceholderKey(final String value) {
        if (Objects.isNull(value)) {
            return null;
        }

        final Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);

        return matcher.find() ? matcher.group(1) : null;
    }
}