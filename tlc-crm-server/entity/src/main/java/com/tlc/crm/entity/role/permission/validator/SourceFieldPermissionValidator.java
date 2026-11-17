package com.tlc.crm.entity.role.permission.validator;

import com.tlc.commons.functions.TriConsumer;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.service.FieldSourceService;

import com.tlc.crm.security.cache.service.FieldPermissionService;
import com.tlc.crm.security.permission.model.AccessKeyBasis;
import com.tlc.crm.security.permission.model.ApiDataExtractionRules;
import com.tlc.crm.security.permission.model.ApiDetails;
import com.tlc.crm.security.permission.model.FieldAccessLevel;
import com.tlc.crm.security.permission.model.FieldPermission;
import com.tlc.crm.security.permission.validator.FieldPermissionValidator;
import com.tlc.crm.security.permission.verifier.response.VerificationResponse;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Validates and enforces field-level permissions for entities associated with a specific {@link Source}.
 * <p>
 * This validator acts as a security enforcement layer for API interactions. It performs two main functions:
 * <ol>
 * <li><b>Request Validation:</b> Ensures that incoming API requests do not attempt to modify fields
 * that are restricted (Read-Only or Hidden) for the current user's role.</li>
 * <li><b>Response Sanitization:</b> Modifies outgoing API responses to remove Hidden fields and
 * flag Read-Only fields based on the configured permissions.</li>
 * </ol>
 * <p>
 * It utilizes {@link ApiDataExtractionRules} to traverse complex JSON structures to locate the specific
 * data fields mapped to the underlying CRM fields.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SourceFieldPermissionValidator implements FieldPermissionValidator {

    public static final Type TYPE = () -> "FIELD";

    private final long orgId;
    private final long roleId;
    private final ApiDetails apiDetails;
    private final FieldSource source;
    private final FieldService fieldService;
    private final FieldPermissionService fieldPermissionService;

    private boolean hasEmptyObjectsInJsonData;

    public SourceFieldPermissionValidator(final long orgId, final long roleId, final ApiDetails apiDetails, final Source source) {
        this.orgId = orgId;
        this.roleId = roleId;
        this.apiDetails = apiDetails;
        this.source = FieldSourceService.getInstance().getSource(source.getName());
        this.fieldService = FieldService.getInstance();
        this.fieldPermissionService = FieldPermissionService.getInstance();
    }

    /**
     * Gets the permission validator type
     *
     * @return the {@link Type}
     */
    @Override
    public Type getType() {
        return TYPE;
    }

    /**
     * Validates the incoming JSON request payload against field permissions.
     * <p>
     * Logic flow:
     * 1. Fetches all field permissions for the current Role and Source.
     * 2. Identifies fields that are NOT {@link FieldAccessLevel#EDITABLE}.
     * 3. Uses {@link ApiDataExtractionRules} to determine how to locate these fields in the JSON.
     * 4. Checks if the JSON contains values for these restricted fields.
     * 5. If a restricted field is present in the payload, an error is added to the {@link VerificationResponse}.
     * </p>
     *
     * @param requestJson The incoming API request body.
     * @param response    The response object to populate with privilege errors if validation fails.
     * @return {@code true} if the request is valid (no privilege violations), {@code false} otherwise.
     */
    @Override
    public boolean validateRequestData(final JsonObject requestJson, final VerificationResponse response) {
        final Collection<FieldPermission> fieldPermissions = fieldPermissionService.getPermissions(orgId, roleId, source.id());
        final Optional<ApiDataExtractionRules> optionalApiDataExtractionRules = fieldPermissionService.getApiDataExtractionDetails(apiDetails);

        if (optionalApiDataExtractionRules.isPresent()) {
            final ApiDataExtractionRules apiDataExtractionRules = optionalApiDataExtractionRules.get();

            if (apiDataExtractionRules.hasRequestDataExtractionRule()) {
                final Collection<ApiDataExtractionRules.Rule> requestDataRules = apiDataExtractionRules.getRequestDataExtractionRules();
                final Map<FieldAccessLevel, Map<Long, FieldPermission>> readOnlyAndHiddenFieldPermissions = fieldPermissions.stream()
                        .filter(fieldPermission -> {
                            final FieldAccessLevel accessLevel = fieldPermission.getAccessLevel();

                            return FieldAccessLevel.HIDDEN == accessLevel || FieldAccessLevel.READ_ONLY == accessLevel;
                        }).collect(Collectors.groupingBy(FieldPermission::getAccessLevel, Collectors.toMap(FieldPermission::getFieldId, Function.identity())));

                if (!readOnlyAndHiddenFieldPermissions.isEmpty()) {
                    final Consumer<FieldAccessLevel> requestDataValidator = (fieldAccessLevel) -> {
                        final Map<Long, FieldPermission> fieldPermissionByFieldIds = readOnlyAndHiddenFieldPermissions.get(fieldAccessLevel);

                        if (Objects.nonNull(fieldPermissionByFieldIds) && !fieldPermissionByFieldIds.isEmpty() && Objects.nonNull(requestDataRules)) {
                            final Map<String, FieldInfo> fieldByKeyNames = groupFieldsByKeyName(apiDataExtractionRules,
                                    fieldPermissionByFieldIds, PresetConfig::requestDataAccessKey);
                            final ApiDataResolver apiDataResolver = new ApiDataResolver(ApiDataResolver.ApiDataType.REQUEST, fieldByKeyNames);

                            apiDataResolver.setVerificationResponse(response);

                            for (final ApiDataExtractionRules.Rule requestDataRule : requestDataRules) {
                                extractAndResolveFieldData(fieldByKeyNames.keySet(), null, requestJson, requestDataRule, apiDataResolver);
                            }
                        }
                    };

                    for (final FieldAccessLevel fieldAccessLevel : apiDataExtractionRules.getAccessLevelsToBeValidated()) {
                        requestDataValidator.accept(fieldAccessLevel);
                    }
                }
                apiDataExtractionRules.getApiTypeToBeMigrated().ifPresent(apiType -> requestJson.put(FieldKeys.TYPE.getName(), apiType));

                return !response.hasErrors();
            }
        }
        return true;
    }

    /**
     * Sanitizes and alters the outgoing JSON response payload based on field permissions.
     * <p>
     * Logic flow:
     * 1. Identifies fields that are {@link FieldAccessLevel#HIDDEN} or {@link FieldAccessLevel#READ_ONLY}.
     * 2. Iterates through the response JSON using the extraction rules.
     * 3. For {@code HIDDEN} fields: The field key and value are completely removed from the JSON.
     * 4. For {@code READ_ONLY} fields: A metadata flag ({@code "readOnly": true}) is injected into the JSON object
     * (typically used by UI clients to disable input).
     * </p>
     *
     * @param responseJson The outgoing API response body to be modified.
     * @return {@code true} indicating processing completed successfully.
     */
    @Override
    public boolean validateAndAlterResponseData(final JsonObject responseJson) {
        final Collection<FieldPermission> fieldPermissions = fieldPermissionService.getPermissions(orgId, roleId, source.id());
        final Optional<ApiDataExtractionRules> optionalApiDataExtractionRules = fieldPermissionService.getApiDataExtractionDetails(apiDetails);

        if (optionalApiDataExtractionRules.isPresent()) {
            final ApiDataExtractionRules apiDataExtractionRules = optionalApiDataExtractionRules.get();

            if (apiDataExtractionRules.hasResponseDataExtractionRules()) {
                final Map<FieldAccessLevel, Collection<ApiDataExtractionRules.Rule>> responseDataRules = apiDataExtractionRules.getResponseDataExtractionRules();
                final Map<FieldAccessLevel, Map<Long, FieldPermission>> fieldPermissionsByAccessLevel = fieldPermissions.stream()
                        .collect(Collectors.groupingBy(FieldPermission::getAccessLevel, Collectors.toMap(FieldPermission::getFieldId, Function.identity())));

                if (!fieldPermissionsByAccessLevel.isEmpty()) {
                    final Consumer<FieldAccessLevel> responseDataValidator = (fieldAccessLevel) -> {

                        final Map<Long, FieldPermission> fieldPermissionByFieldIds = fieldPermissionsByAccessLevel.get(fieldAccessLevel);
                        final Collection<ApiDataExtractionRules.Rule> rules = responseDataRules.get(fieldAccessLevel);

                        if (Objects.nonNull(fieldPermissionByFieldIds) && !fieldPermissionByFieldIds.isEmpty() && Objects.nonNull(rules)) {
                            final Map<String, FieldInfo> fieldByKeyNames = groupFieldsByKeyName(apiDataExtractionRules,
                                    fieldPermissionByFieldIds, PresetConfig::responseDataAccessKey);
                            final ApiDataResolver apiDataResolver = new ApiDataResolver(ApiDataResolver.ApiDataType.RESPONSE, fieldByKeyNames);

                            for (final ApiDataExtractionRules.Rule responseDataRule : rules) {
                                extractAndResolveFieldData(fieldByKeyNames.keySet(), null, responseJson, responseDataRule, apiDataResolver);
                            }
                        }
                    };

                    for (final FieldAccessLevel fieldAccessLevel : apiDataExtractionRules.getAccessLevelsToBeValidated()) {
                        responseDataValidator.accept(fieldAccessLevel);
                    }

                    if (hasEmptyObjectsInJsonData) {
                        responseJson.removeEmptyObjects();
                    }
                }
            }
        }
        return true;
    }

    /**
     * Groups and maps field metadata into a keyed Map based on the specified extraction rules.
     *
     * @param apiDataExtractionRules    Configuration defining how the keys should be formatted (ID vs. ALIAS_NAME).
     * @param fieldPermissionByFieldIds A map of field IDs representing the scope of fields to be processed.
     * @return A {@link Map} where the key is the resolved string identifier and the value is the corresponding {@link FieldInfo} object.
     */
    private Map<String, FieldInfo> groupFieldsByKeyName(final ApiDataExtractionRules apiDataExtractionRules,
                                                        final Map<Long, FieldPermission> fieldPermissionByFieldIds,
                                                        final Function<PresetConfig, String> dataAccessKeyProvider) {
        final AccessKeyBasis accessKeyBasis = apiDataExtractionRules.getAccessKeyBasis();
        final Collection<FieldInfo> fields = fieldService.getFieldsById(orgId, fieldPermissionByFieldIds.keySet()).values();

        return fields.stream()
                .filter(field -> !field.presetConfig().isPrimaryField())
                .collect(Collectors.toMap(field -> switch (accessKeyBasis) {
                    case ID -> field.idAsString();
                    case ALIAS_NAME -> {
                        final String dataAccessKey = dataAccessKeyProvider.apply(field.presetConfig());

                        yield Objects.isNull(dataAccessKey) ? field.aliasName() : dataAccessKey;
                    }
                }, Function.identity()));
    }

    /**
     * Recursively extracts and resolves field data from a JSON structure based on defined extraction rules.
     * * <p>The method processes data extraction in three priority levels:
     * 1. Configuration object extraction (configObjectExtractionRule)
     * 2. Child object traversal (childObjectExtractionRule)
     * 3. Terminal field resolution and unauthorized field tracking
     * </p>
     *
     * @param fieldKeys          The collection of keys to be extracted/validated.
     * @param parentJsonData     The source JSON data from the parent level.
     * @param jsonData           The current JSON object being processed.
     * @param dataExtractionRule The ruleset defining how to navigate and extract data.
     * @param apiDataResolver    The resolver responsible for finalizing data and logging unauthorized fields.
     */
    private void extractAndResolveFieldData(final Collection<String> fieldKeys, final Object parentJsonData, final JsonObject jsonData,
                                            final ApiDataExtractionRules.Rule dataExtractionRule, final ApiDataResolver apiDataResolver) {
        final TriConsumer<Collection<String>, Object, JsonObject> fieldDataResolver = (fieldReferenceKeys, parentObject, jsonObject) -> {
            if (dataExtractionRule.hasChildObjectExtractionRule()) {
                extractAndResolveFieldData(fieldReferenceKeys, parentObject, jsonObject, dataExtractionRule.getChildObjectExtractionRule(), apiDataResolver);
            } else if (Objects.isNull(fieldReferenceKeys) || fieldReferenceKeys.stream().filter(jsonObject::containsKey)
                    .map(apiDataResolver::addUnauthorizedField).findAny().isPresent()) {

                if (Objects.nonNull(parentObject)) {
                    apiDataResolver.setParentObject(parentObject);
                }
                apiDataResolver.resolve(jsonObject);
            }
        };

        if (dataExtractionRule.hasObjectKey()) {
            final String parentObjectKey = dataExtractionRule.getObjectKey();

            if (jsonData.containsKey(parentObjectKey)) {
                final Object parentObject = dataExtractionRule.getParentObject(jsonData);

                extractAndResolveFieldData(fieldKeys, parentObject, parentJsonData, jsonData, apiDataResolver, fieldDataResolver);
            }
        } else {
            for (final String fieldKey : fieldKeys) {
                if (jsonData.containsKey(fieldKey)) {
                    extractAndResolveFieldData(fieldKeys, dataExtractionRule.getParentObject(fieldKey, jsonData), parentJsonData, jsonData, apiDataResolver, fieldDataResolver);
                }
            }
        }
    }

    /**
     * Dispatches the extraction logic based on the type of the parentObject.
     * * <ul>
     * <li><b>JsonArray:</b> Iterates through elements (handled by extractFieldJsonArray).</li>
     * <li><b>JsonObject:</b> Directly accepts the object for resolution.</li>
     * <li><b>String:</b> Treats the object as a single field and validates authorization.</li>
     * </ul>
     *
     * @param fieldKeys         The collection of keys to be extracted.
     * @param parentObject      The current object context (could be JsonArray, JsonObject, or String).
     * @param parentJsonData    The JSON data from the preceding hierarchy level.
     * @param jsonData          The current active JSON object.
     * @param apiDataResolver   The resolver for unauthorized field tracking.
     * @param fieldDataResolver The functional callback to process the resolved data.
     */
    private void extractAndResolveFieldData(final Collection<String> fieldKeys, final Object parentObject, final Object parentJsonData,
                                            final JsonObject jsonData, final ApiDataResolver apiDataResolver,
                                            final TriConsumer<Collection<String>, Object, JsonObject> fieldDataResolver) {
        if (parentObject instanceof final JsonArray jsonArray) {
            extractFieldJsonArray(fieldKeys, jsonArray, fieldDataResolver);
        } else if (parentObject instanceof final JsonObject jsonObject) {
            fieldDataResolver.accept(fieldKeys, jsonData, jsonObject);
        } else if (parentObject instanceof final String field) {
            if (fieldKeys.contains(field)) {
                apiDataResolver.addUnauthorizedField(field);
                fieldDataResolver.accept(null, parentJsonData, jsonData);
            }
        }
    }

    /**
     * Helper method to traverse a JSON array recursively.
     * Delegates processing of {@link JsonObject} elements to the consumer and handles nested arrays
     * to ensure deep traversal of the payload.
     *
     * @param fieldKeys              the field identifier contexts.
     * @param jsonArray              the array to iterate.
     * @param fieldValueExistChecker the action to perform on found objects.
     */
    private void extractFieldJsonArray(final Collection<String> fieldKeys, final JsonArray jsonArray,
                                       final TriConsumer<Collection<String>, Object, JsonObject> fieldValueExistChecker) {
        int size = jsonArray.size();

        for (int index = 0; index < size; index++) {
            final int currentSize = jsonArray.size();

            index -= size - currentSize;
            size = currentSize;
            final Object data = jsonArray.get(index);

            if (data instanceof Map<?, ?>) {
                fieldValueExistChecker.accept(fieldKeys, jsonArray, jsonArray.getJsonObject(index));
            } else if (data instanceof List<?>) {
                extractFieldJsonArray(fieldKeys, jsonArray.getJsonArray(index), fieldValueExistChecker);
            }
        }
    }

    /**
     * Internal helper class responsible for resolving and enforcing data access policies on JSON objects.
     * It handles field-level security for both incoming REQUESTs (validation) and outgoing RESPONSEs (data masking/filtering).
     *
     * @author Sathish
     * @version 1.0
     */
    private class ApiDataResolver {

        private final Map<String, FieldInfo> fieldByKeyNames;
        private final ApiDataType apiDataType;

        private VerificationResponse verificationResponse;
        private Object parentObject;
        private Collection<String> unauthorizedFields;

        private ApiDataResolver(final ApiDataType apiDataType, final Map<String, FieldInfo> fieldByKeyNames) {
            this.apiDataType = apiDataType;
            this.fieldByKeyNames = fieldByKeyNames;
        }

        public void setParentObject(final Object parentObject) {
            this.parentObject = parentObject;
        }

        public void setVerificationResponse(final VerificationResponse verificationResponse) {
            this.verificationResponse = verificationResponse;
        }

        /**
         * Captures a field name that the user does not have permission to access.
         * Initializes the collection lazily if it's the first field found.
         *
         * @param fieldName The technical key of the field.
         * @return The fieldName provided (useful for functional mapping).
         */
        public String addUnauthorizedField(final String fieldName) {
            unauthorizedFields = Objects.isNull(unauthorizedFields) ? new HashSet<>() : unauthorizedFields;

            unauthorizedFields.add(fieldName);
            return fieldName;
        }

        /**
         * The core execution method of the resolver.
         *
         * @param jsonObject The data chunk to be resolved.
         */
        public void resolve(final JsonObject jsonObject) {
            if (Objects.nonNull(apiDataType)) {
                switch (apiDataType) {
                    case REQUEST -> {
                        if (Objects.nonNull(unauthorizedFields)) {
                            unauthorizedFields.stream()
                                    .map(fieldByKeyNames::get).filter(Objects::nonNull)
                                    .map(FieldInfo::i18nName).forEach(verificationResponse::addFieldPrivilegeError);
                        }
                    }
                    case RESPONSE -> {
                        if (Objects.nonNull(unauthorizedFields)) {
                            if (Objects.isNull(parentObject)) {
                                unauthorizedFields.forEach(jsonObject::remove);

                                if (!hasEmptyObjectsInJsonData) {
                                    hasEmptyObjectsInJsonData = jsonObject.size() == 0;
                                }
                                return;
                            }

                            if (parentObject instanceof JsonArray parentJsonArray) {
                                parentJsonArray.remove(jsonObject);

                                if (!hasEmptyObjectsInJsonData) {
                                    hasEmptyObjectsInJsonData = parentJsonArray.size() == 0;
                                }
                            } else if (parentObject instanceof JsonObject parentJsonObject) {
                                unauthorizedFields.forEach(parentJsonObject::remove);

                                if (!hasEmptyObjectsInJsonData) {
                                    hasEmptyObjectsInJsonData = parentJsonObject.size() == 0;
                                }
                            }
                        }
                        unauthorizedFields = null;
                    }
                }
            }
        }

        /**
         * Represents the direction of data flow, determining whether the resolver
         * performs validation (REQUEST) or data scrubbing (RESPONSE).
         */
        private enum ApiDataType {

            REQUEST, RESPONSE
        }
    }
}