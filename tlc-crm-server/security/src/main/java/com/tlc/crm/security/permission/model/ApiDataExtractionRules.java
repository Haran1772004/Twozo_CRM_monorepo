package com.tlc.crm.security.permission.model;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

/**
 * Defines the rules and strategies for extracting data from API payloads to support permission validation.
 * <p>
 * This class serves as a configuration bridge between raw API JSON payloads (Requests and Responses)
 * and the security system. It allows the definition of hierarchical rules to locate specific data points,
 * such as Access Keys (IDs or Aliases), even within complex nested JSON structures.
 * </p>
 * <p>
 * It supports separate extraction strategies for:
 * <ul>
 * <li><b>Request Data:</b> Single rule path to locate incoming data.</li>
 * <li><b>Response Data:</b> A map of rules keyed by {@link FieldAccessLevel}, allowing different extraction logic
 * based on whether a field is HIDDEN or READ_ONLY.</li>
 * </ul>
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class ApiDataExtractionRules {

    private static final String ACCESS_KEY_BASIS = "accessKeyBasis";
    private static final String REQUEST_DATA_EXTRACTION_RULES = "requestDataExtractionRules";
    private static final String RESPONSE_DATA_EXTRACTION_RULES = "responseDataExtractionRules";
    private static final String API_TYPE_TO_BE_MIGRATED = "apiTypeToBeMigrated";
    private static final String ACCESS_LEVELS_TO_BE_VALIDATED = "accessLevelsToBeValidated";

    private final ApiDetails apiDetails;
    private final AccessKeyBasis accessKeyBasis;
    private final String apiTypeToBeMigrated;
    private final Collection<Rule> requestDataExtractionRules;
    private final Map<FieldAccessLevel, Collection<Rule>> responseDataExtractionRules;
    private final Collection<FieldAccessLevel> accessLevelsToBeValidated;
    private final boolean hasRequestDataExtractionRule;
    private final boolean hasResponseDataExtractionRules;

    /**
     * Constructs a new instance by parsing the provided JSON configuration.
     *
     * @param apiDetails the metadata of the API endpoint associated with these rules.
     * @param configJson the {@link JsonObject} containing the rule definitions. Expected keys:
     * <ul>
     * <li>{@code accessKeyBasis}: Short ID for {@link AccessKeyBasis} (ID vs Alias).</li>
     * <li>{@code requestDataExtractionRules}: (Optional) JSON object defining the request path.</li>
     * <li>{@code responseDataExtractionRules}: (Optional) JSON object mapping Access Levels to rule definitions.</li>
     * </ul>
     */
    public ApiDataExtractionRules(final ApiDetails apiDetails, final JsonObject configJson) {
        this.apiDetails = apiDetails;
        this.accessKeyBasis = AccessKeyBasis.get(configJson.getShort(ACCESS_KEY_BASIS));
        this.hasRequestDataExtractionRule = configJson.containsKey(REQUEST_DATA_EXTRACTION_RULES);
        this.requestDataExtractionRules = hasRequestDataExtractionRule
                ? buildDataExtractionRules(configJson.getJsonArray(REQUEST_DATA_EXTRACTION_RULES)) : null;
        this.hasResponseDataExtractionRules = configJson.containsKey(RESPONSE_DATA_EXTRACTION_RULES);
        this.responseDataExtractionRules = hasResponseDataExtractionRules
                ? buildResponseDataExtractionRules(configJson.getJsonObject(RESPONSE_DATA_EXTRACTION_RULES)) : null;
        this.apiTypeToBeMigrated = configJson.getNullableString(API_TYPE_TO_BE_MIGRATED);
        this.accessLevelsToBeValidated = collectAccessLevels(configJson.getJsonArray(ACCESS_LEVELS_TO_BE_VALIDATED));
    }

    /**
     * Returns the API details associated with this rule set.
     *
     * @return the {@link ApiDetails}.
     */
    public ApiDetails getApiDetails() {
        return apiDetails;
    }

    /**
     * Returns the basis used for identifying fields (e.g., by ID or by Alias name).
     *
     * @return the {@link AccessKeyBasis}.
     */
    public AccessKeyBasis getAccessKeyBasis() {
        return accessKeyBasis;
    }

    /**
     * Gets the rule configured for extracting data from the API request body.
     *
     * @return the request {@link Rule}, or null if not configured.
     */
    public Collection<Rule> getRequestDataExtractionRules() {
        return requestDataExtractionRules;
    }

    /**
     * Gets the map of rules configured for extracting/modifying data in the API response body.
     *
     * @return a Map of response {@link Rule}s keyed by {@link FieldAccessLevel}, or null if not configured.
     */
    public Map<FieldAccessLevel, Collection<Rule>> getResponseDataExtractionRules() {
        return responseDataExtractionRules;
    }

    /**
     * Retrieves the set of access levels that require validation during the current authorization lifecycle.
     *
     * @return A {@link Collection} of {@link FieldAccessLevel} constants representing the permissions required to perform the requested operation.
     */
    public Collection<FieldAccessLevel> getAccessLevelsToBeValidated() {
        return accessLevelsToBeValidated;
    }

    /**
     * Checks if a request data extraction rule is present.
     *
     * @return {@code true} if configured, {@code false} otherwise.
     */
    public boolean hasRequestDataExtractionRule() {
        return hasRequestDataExtractionRule;
    }

    /**
     * Checks if response data extraction rules are present.
     *
     * @return {@code true} if configured, {@code false} otherwise.
     */
    public boolean hasResponseDataExtractionRules() {
        return hasResponseDataExtractionRules;
    }

    /**
     * Gets the API type targeted for the migration process.
     *
     * @return the migration API type, wrapped in an Optional.
     */
    public Optional<String> getApiTypeToBeMigrated() {
        return Optional.ofNullable(apiTypeToBeMigrated);
    }

    /**
     * Parses a JSON array of strings into a collection of {@link FieldAccessLevel} constants.
     *
     * @param accessLevelsArray A {@link JsonArray} containing the names of access levels.
     * @return A {@link Collection} of {@link FieldAccessLevel} constants representing the parsed permissions.
     */
    private Collection<FieldAccessLevel> collectAccessLevels(final JsonArray accessLevelsArray) {
        final Collection<FieldAccessLevel> fieldAccessLevels = new HashSet<>();
        final int size = accessLevelsArray.size();

        for (int index = 0; index < size; index++) {
            fieldAccessLevels.add(FieldAccessLevel.valueOf(accessLevelsArray.getString(index)));
        }
        return fieldAccessLevels;
    }

    /**
     * Converts a JSON array of rule configurations into a collection of {@link Rule} objects.
     *
     * @param requestDataExtractionRulesJson the source JSON array to parse.
     * @return a collection containing the parsed rules.
     */
    private Collection<Rule> buildDataExtractionRules(final JsonArray requestDataExtractionRulesJson) {
        final Collection<Rule> requestDataExtractionRules = new ArrayList<>();
        final int size = requestDataExtractionRulesJson.size();

        for (int index = 0; index < size; index++) {
            requestDataExtractionRules.add(new Rule(requestDataExtractionRulesJson.getJsonObject(index)));
        }
        return requestDataExtractionRules;
    }

    /**
     * Helper method to parse the response data extraction rules map.
     * Converts JSON keys (Access Level Strings) into {@link FieldAccessLevel} enums.
     *
     * @param responseDataExtractionRulesJson the JSON object containing response rules.
     * @return a Map associating Access Levels with their specific extraction Rules.
     */
    private Map<FieldAccessLevel, Collection<Rule>> buildResponseDataExtractionRules(final JsonObject responseDataExtractionRulesJson) {
        final Map<FieldAccessLevel, Collection<Rule>> responseDataExtractionRules = new HashMap<>();

        for (final String accessLevel : responseDataExtractionRulesJson.keySet()) {
            final FieldAccessLevel fieldAccessLevel = FieldAccessLevel.valueOf(accessLevel);

            responseDataExtractionRules.put(fieldAccessLevel, buildDataExtractionRules(responseDataExtractionRulesJson.getJsonArray(accessLevel)));
        }

        return responseDataExtractionRules;
    }

    /**
     * Represents a single node or step in the data extraction path.
     * <p>
     * A Rule defines how to navigate one level of a JSON structure. It supports recursion via
     * child rules and handles different JSON types (Object, Array, String).
     * </p>
     */
    public static class Rule {

        private static final String OBJECT_KEY = "objectKey";
        private static final String OBJECT_TYPE = "objectType";
        private static final String CHILD_OBJECT_EXTRACTION_RULE = "childObjectExtractionRule";

        private final boolean hasObjectKey;
        private final String objectKey;
        private final JsonDataFetcher.ObjectType objectType;
        private final JsonDataFetcher jsonDataFetcher;
        private final boolean hasChildObjectExtractionRule;
        private final Rule childObjectExtractionRule;

        /**
         * Private constructor to build a Rule from JSON.
         *
         * @param ruleJson the JSON configuration for this rule node.
         */
        private Rule(final JsonObject ruleJson) {
            hasObjectKey = ruleJson.containsKey(OBJECT_KEY);
            objectKey = ruleJson.getNullableString(OBJECT_KEY);
            objectType = ruleJson.containsKey(OBJECT_TYPE) ? JsonDataFetcher.ObjectType.valueOf(ruleJson.getString(OBJECT_TYPE)) : JsonDataFetcher.ObjectType.JSON_OBJECT;
            jsonDataFetcher = hasObjectKey ? new JsonDataFetcher(objectKey, objectType) : null;
            hasChildObjectExtractionRule = ruleJson.containsKey(CHILD_OBJECT_EXTRACTION_RULE);
            childObjectExtractionRule = hasChildObjectExtractionRule ? new Rule(ruleJson.getJsonObject(CHILD_OBJECT_EXTRACTION_RULE)) : null;
        }

        /**
         * Checks if there is an object key.
         *
         * @return {@code true} if an object key exists.
         */
        public boolean hasObjectKey() {
            return hasObjectKey;
        }

        /**
         * Gets the JSON key name for the object at this rule level.
         *
         * @return the object key string.
         */
        public String getObjectKey() {
            return objectKey;
        }

        /**
         * Checks if there is a nested rule for a child object.
         *
         * @return {@code true} if a child rule exists.
         */
        public boolean hasChildObjectExtractionRule() {
            return hasChildObjectExtractionRule;
        }

        /**
         * Gets the recursive rule for the child object.
         *
         * @return the child {@link Rule}, or null.
         */
        public Rule getChildObjectExtractionRule() {
            return childObjectExtractionRule;
        }

        /**
         * Extracts the parent object context from the provided JSON data using the
         * pre-configured {@code JsonDataFetcher}.
         *
         * @param <T>      The expected return type (e.g., JsonObject, JsonArray, or String).
         * @param jsonData The source JSON data to extract from.
         * @return The extracted parent object cast to the requested type {@code T}.
         */
        public <T> T getParentObject(final JsonObject jsonData) {
            return jsonDataFetcher.fetch(jsonData);
        }

        /**
         * Dynamically creates a fetcher to extract a parent object from the JSON data
         * using a specific key. This is useful when the key is determined at runtime
         * rather than during initial rule configuration.
         *
         * @param <T>       The expected return type.
         * @param objectKey The specific JSON key to look up.
         * @param jsonData  The source JSON data.
         * @return The value associated with the key, cast to the requested type {@code T}.
         */
        public <T> T getParentObject(final String objectKey, final JsonObject jsonData) {
            return new JsonDataFetcher(objectKey, objectType).fetch(jsonData);
        }

        /**
         * A utility class designed to fetch specific data types from a {@link JsonObject}.
         * It maps a JSON key to a specific {@link ObjectType} to ensure the retrieved
         * data matches the expected format defined in the extraction rules.
         *
         * @author Sathish
         * @version 1.0
         */
        private class JsonDataFetcher {

            private final String objectKey;
            private final ObjectType objectType;

            private JsonDataFetcher(final String objectKey, final ObjectType objectType) {
                this.objectKey = objectKey;
                this.objectType = objectType;
            }

            /**
             * Retrieves and casts data from the provided {@link JsonObject} based on the configured {@code objectType}.
             *
             * @param <T>      The expected return type (String, JsonObject, or JsonArray).
             * @param jsonData The source JSON object containing the data.
             * @return The value associated with {@code objectKey}, cast to type {@code T}.
             * @throws ClassCastException if the actual data type does not match the requested type {@code T}.
             */
            @SuppressWarnings("unchecked")
            private <T> T fetch(final JsonObject jsonData) {
                return (T) switch (objectType) {
                    case STRING -> jsonData.getString(objectKey);
                    case JSON_OBJECT -> jsonData.getJsonObject(objectKey);
                    case JSON_ARRAY -> jsonData.getJsonArray(objectKey);
                };
            }

            /**
             * Defines the expected JSON data type for the current rule node.
             */
            private enum ObjectType {

                JSON_OBJECT, JSON_ARRAY, STRING
            }
        }
    }
}