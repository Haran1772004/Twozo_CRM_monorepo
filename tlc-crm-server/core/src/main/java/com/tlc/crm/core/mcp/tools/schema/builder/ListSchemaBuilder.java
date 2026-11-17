package com.tlc.crm.core.mcp.tools.schema.builder;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.filter.api.models.DateInterval;
import com.tlc.crm.entity.filter.api.models.DateIntervalGroup;
import com.tlc.crm.entity.filter.fields.DateIntervalAttributes;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.filter.ComparatorService;
import com.tlc.crm.entity.service.filter.DateIntervalService;

import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.fields.service.FieldTypeService;

import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Shared builder for MCP list schema responses.
 * Centralizes filter field discovery, comparator resolution, and criteria documentation.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class ListSchemaBuilder {

    private static final ListSchemaBuilder INSTANCE = new ListSchemaBuilder();

    private final FieldSourceService fieldSourceService;
    private final FieldService fieldService;
    private final ComparatorService comparatorService;
    private final FieldTypeService fieldTypeService;
    private final DateIntervalService dateIntervalService;
    private final I18nService i18nService;

    private ListSchemaBuilder() {
        this.fieldSourceService = FieldSourceService.getInstance();
        this.fieldService = FieldService.getInstance();
        this.comparatorService = ComparatorService.getInstance();
        this.fieldTypeService = FieldTypeService.getInstance();
        this.dateIntervalService = DateIntervalService.getInstance();
        this.i18nService = I18nServiceProvider.get();
    }

    /**
     * Returns the singleton instance.
     *
     * @return the shared builder instance
     */
    public static ListSchemaBuilder getInstance() {
        return INSTANCE;
    }

    /**
     * Resolves a module name to its {@link FieldSource}, or empty if unknown.
     *
     * @param orgId  organization ID
     * @param module module name (contact, company, product, deal)
     * @return optional field source
     */
    public Optional<FieldSource> resolveSource(final long orgId, final String module) {
        if (module == null || module.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(fieldSourceService.retrieveSourceWithSettings(orgId, module));
    }

    /**
     * Builds the merged list schema with filter fields and embedded comparators.
     *
     * @param orgId  organization ID
     * @param module module name
     * @return JSON response object
     */
    public JsonObject buildListSchema(final long orgId, final String module) {
        final FieldSource source = resolveSource(orgId, module)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Unknown module: '%s'. Valid values: contact, company, product or deal.", module)));
        final Collection<FieldInfo> fields = getFilterableFields(orgId, source);
        final Map<Long, Collection<Comparator>> comparatorsByFieldTypeId = new HashMap<>();
        final JsonArray fieldsArray = Json.array();

        for (final FieldInfo fieldInfo : fields) {
            fieldsArray.put(buildFieldSchema(fieldInfo, comparatorsByFieldTypeId, true));
        }

        final JsonObject response = Json.orderedObject();

        final I18nResolver i18nResolver = i18nService.getResolver(Locale.getDefault());
        final JsonArray dateIntervals = buildDateIntervalsJson(source, i18nResolver);

        response.put("module", module);
        response.put("total_fields", fieldsArray.size());
        response.put("fields", fieldsArray);
        response.put("dateIntervals", dateIntervals);
        response.put("criteriaStructure", getCriteriaStructureDoc());

        return response;
    }

    /**
     * Builds date intervals array for list schema (id, name, label, isRange).
     *
     * @param source       the field source
     * @param i18nResolver the I18nResolver for label translation
     */
    private JsonArray buildDateIntervalsJson(final FieldSource source, final I18nResolver i18nResolver) {
        final JsonArray jsonArray = Json.array();
        final Map<DateIntervalGroup, Map<Long, DateInterval>> groupedDateIntervals = dateIntervalService.getSourceIntervals(true, source.id());

        if (Objects.nonNull(groupedDateIntervals) && !groupedDateIntervals.isEmpty()) {
            groupedDateIntervals.forEach((dateIntervalGroup, dateIntervals) ->
                    dateIntervals.forEach((id, dateInterval) -> {
                        final JsonObject dateIntervalObj = Json.orderedObject();

                        dateIntervalObj.put("id", id);
                        dateIntervalObj.put("name", dateInterval.getName());
                        dateIntervalObj.put("label", i18nResolver.get(dateInterval.getDisplayName()));
                        dateIntervalObj.put(DateIntervalAttributes.IS_RANGE.getName(), dateInterval.isRange());
                        jsonArray.put(dateIntervalObj);
                    }));
        }

        return jsonArray;
    }

    /**
     * Returns filterable fields for the given source.
     *
     * @param orgId  organization ID
     * @param source field source
     */
    private Collection<FieldInfo> getFilterableFields(final long orgId, final FieldSource source) {
        return fieldService.getFieldsBySource(orgId, source.getName()).stream()
                .filter(fieldInfo -> !fieldInfo.storeType().isTextType() && fieldInfo.presetConfig().isFilterable())
                .toList();
    }

    /**
     * Builds a field schema entry, optionally including comparators and criteria example.
     *
     * @param fieldInfo                the field information
     * @param comparatorsByFieldTypeId cache of comparators by field type ID
     * @param includeComparators       whether to include comparators and criteria example
     * @return JSON object representing the field schema
     */
    private JsonObject buildFieldSchema(final FieldInfo fieldInfo,
                                        final Map<Long, Collection<Comparator>> comparatorsByFieldTypeId,
                                        final boolean includeComparators) {
        final JsonObject jsonObject = Json.orderedObject();
        final StoreType storeType = fieldInfo.storeType();
        final long fieldTypeId = fieldTypeService.getFieldTypeByStoreType(storeType).id();

        jsonObject.put("fieldName", fieldInfo.getInternalName());
        jsonObject.put("aliasName", fieldInfo.aliasName());
        jsonObject.put("label", fieldInfo.fieldName());
        jsonObject.put("description", getFieldDescription(storeType));
        jsonObject.put("fieldId", fieldInfo.id());
        jsonObject.put("fieldTypeId", fieldTypeId);
        jsonObject.put("isSortable", isSortable(storeType));

        if (includeComparators) {
            final Collection<Comparator> comparators = getComparatorsForFieldType(fieldTypeId, comparatorsByFieldTypeId);

            jsonObject.put("valueFormat", getValueFormatDescription(storeType));
            jsonObject.put("comparators", buildComparatorsArray(comparators));
            jsonObject.put("criteriaExample", buildCriteriaExample(fieldInfo.idAsString(), storeType, comparators));
        }

        return jsonObject;
    }

    /**
     * Resolves comparators for a field type, caching by fieldTypeId within a single request.
     */
    private Collection<Comparator> getComparatorsForFieldType(final long fieldTypeId,
                                                              final Map<Long, Collection<Comparator>> comparatorsByFieldTypeId) {
        return comparatorsByFieldTypeId.computeIfAbsent(fieldTypeId, comparatorService::getComparatorsByType);
    }

    /**
     * Converts comparators to a JSON array.
     */
    private JsonArray buildComparatorsArray(final Collection<Comparator> comparators) {
        final JsonArray comparatorsArray = Json.array();

        if (comparators != null) {
            for (final Comparator comparator : comparators) {
                final JsonObject compJson = Json.object();

                compJson.put("id", comparator.getId());
                compJson.put("name", comparator.getKeyName());
                compJson.put("alias", comparator.getAliasName());
                compJson.put("needsValue", comparator.isValueNeeded());
                compJson.put("multiValue", comparator.isMultiValueAllowed());
                comparatorsArray.put(compJson);
            }
        }

        return comparatorsArray;
    }

    /**
     * Determines whether the specified field type supports sorting.
     *
     * @param storeType the store type of the field
     */
    private boolean isSortable(final StoreType storeType) {
        return switch (storeType) {
            case SINGLE_STRING, SINGLE_NUMBER, DATE, TIME, IDENTIFIER, DROPDOWN, CONTACT, COMPANY, COMPANY_ONBOARD,
                 CONTACT_ONBOARD, PHONE, EMAIL, MONETARY, TIME_RANGE, DATE_RANGE, DATE_TIME_RANGE,
                 ADDRESS, DEAL_VALUE, TERRITORY, DATE_TIME, EMAILS, PHONES, DEAL, OWNER, AUTO_NUMBER -> true;
            default -> false;
        };
    }

    /**
     * Returns a generic description for the specified field type.
     *
     * @param storeType the store type of the field
     */
    private String getFieldDescription(final StoreType storeType) {
        return switch (storeType) {
            case DROPDOWN, MULTI_SELECT -> """
                    Call get_field_options with fieldName=<fieldName>' and query='<desired_name>' to search for the required option.
                    If the response indicates hasMore=true, continue fetching using the returned cursor until the desired option is found or no more results are available.
                    Use the ID of the selected option in the request payload for this field.
                    """;
            case OWNER, OWNERS -> """
                    Call search_users with optional query='<desired_name>' to search for the required user.
                    If the response indicates hasMore=true, continue fetching using the returned cursor until the desired user is found or no more results are available.
                    Use the ID of the selected user in the request payload for this field.
                    """;
            case TERRITORY -> """
                    Call get_field_options with fieldName='territory' and query='<desired_name>' to search for the required option.
                    If the response indicates hasMore=true, continue fetching using the returned cursor until the desired option is found or no more results are available.
                    Use the ID of the selected option in the request payload for this field.
                    """;
            case CONTACT_ONBOARD, CONTACTS_ONBOARD, CONTACT, COMPANY_ONBOARD, COMPANY -> """
                    Call get_field_options with fieldName='<fieldName>' and query='<desired_name>' to search for the required option.
                    If the response indicates hasMore=true, continue fetching using the returned cursor until the desired option is found or no more results are available.
                    Use the ID of the selected option in the request payload for this field.
                    """;
            case TAGS -> """
                    Call get_field_options with fieldName='tags' and query='<desired_name>' to search for the required option.
                    If the response indicates hasMore=true, continue fetching using the returned cursor until the desired option is found or no more results are available.
                    Use the IDs of the selected option in the request payload for this field.
                    """;
            case TIME, TIME_RANGE -> """
                    Select from the available intervals. Intervals:
                    1: 12:00 AM, 2: 12:15 AM, 3: 12:30 AM, 4: 12:45 AM, 5: 01:00 AM, 6: 01:15 AM, 7: 01:30 AM, 8: 01:45 AM, 9: 02:00 AM, 10: 02:15 AM, 11: 02:30 AM, 12: 02:45 AM, 13: 03:00 AM, 14: 03:15 AM, 15: 03:30 AM, 16: 03:45 AM, 17: 04:00 AM, 18: 04:15 AM, 19: 04:30 AM, 20: 04:45 AM,
                    21: 05:00 AM, 22: 05:15 AM, 23: 05:30 AM, 24: 05:45 AM, 25: 06:00 AM, 26: 06:15 AM, 27: 06:30 AM, 28: 06:45 AM, 29: 07:00 AM, 30: 07:15 AM, 31: 07:30 AM, 32: 07:45 AM, 33: 08:00 AM, 34: 08:15 AM, 35: 08:30 AM, 36: 08:45 AM, 37: 09:00 AM, 38: 09:15 AM, 39: 09:30 AM,
                    40: 09:45 AM, 41: 10:00 AM, 42: 10:15 AM, 43: 10:30 AM, 44: 10:45 AM, 45: 11:00 AM, 46: 11:15 AM, 47: 11:30 AM, 48: 11:45 AM, 49: 12:00 PM, 50: 12:15 PM, 51: 12:30 PM, 52: 12:45 PM, 53: 01:00 PM, 54: 01:15 PM, 55: 01:30 PM, 56: 01:45 PM, 57: 02:00 PM, 58: 02:15 PM, 59: 02:30 PM, 60: 02:45 PM,
                    61: 03:00 PM, 62: 03:15 PM, 63: 03:30 PM, 64: 03:45 PM, 65: 04:00 PM, 66: 04:15 PM, 67: 04:30 PM, 68: 04:45 PM, 69: 05:00 PM, 70: 05:15 PM, 71: 05:30 PM, 72: 05:45 PM, 73: 06:00 PM, 74: 06:15 PM, 75: 06:30 PM, 76: 06:45 PM, 77: 07:00 PM, 78: 07:15 PM, 79: 07:30 PM, 80: 07:45 PM,
                    81: 08:00 PM, 82: 08:15 PM, 83: 08:30 PM, 84: 08:45 PM, 85: 09:00 PM, 86: 09:15 PM, 87: 09:30 PM, 88: 09:45 PM, 89: 10:00 PM, 90: 10:15 PM, 91: 10:30 PM, 92: 10:45 PM, 93: 11:00 PM, 94: 11:15 PM, 95: 11:30 PM, 96: 11:45 PM
                    """;
            case DATE, DATE_RANGE, DATE_TIME, DATE_TIME_RANGE -> """
                    Use valueEntityId from dateIntervals in get_list_schema.
                    For common relative filters (today, yesterday, last month, this week, this month), use dateInterval on list_* directly.
                    """;
            default -> "Text value";
        };
    }

    /**
     * Returns a human-readable description of the expected value format for filter criteria.
     *
     * @param storeType the store type of the field
     */
    private String getValueFormatDescription(final StoreType storeType) {
        if (storeType == null) {
            return "Plain text string value. Example: {value: text}";
        }

        return switch (storeType) {
            case SINGLE_STRING -> "Plain text string value. Example: {value: John}";
            case SINGLE_NUMBER -> "Numeric value as string. Example: {value: 100}";
            case DROPDOWN, CONTACT, COMPANY, CONTACT_ONBOARD, CONTACTS_ONBOARD, COMPANY_ONBOARD ->
                    "Option/entity ID as valueEntityId. Example: {value: optionName, valueEntityId: 123}. Use get_field_options('fieldName' = <fieldName>) to find valid IDs.";
            case OWNER ->
                    "Option/entity ID as valueEntityId. Example: {value: optionName, valueEntityId: 123}. Use search_users tool to find valid IDs.";
            case MULTI_SELECT ->
                    "Multiple option IDs. Pass multiple value objects: [{valueEntityId: 1}, {valueEntityId: 2}]. Use get_field_options('fieldName' = <fieldName>) to find valid IDs.";
            case DATE, DATE_TIME, DATE_RANGE, DATE_TIME_RANGE ->
                    "Use dateIntervals from get_list_schema. Pass valueEntityId from dateIntervals ID. Example: {value: Today, valueEntityId: 23}. For createdTime filters, use dateInterval on list_* directly.";
            case TIME, TIME_RANGE -> """
                    Select from the available intervals. Intervals:
                    1: 12:00 AM, 2: 12:15 AM, 3: 12:30 AM, 4: 12:45 AM, 5: 01:00 AM, 6: 01:15 AM, 7: 01:30 AM, 8: 01:45 AM, 9: 02:00 AM, 10: 02:15 AM, 11: 02:30 AM, 12: 02:45 AM, 13: 03:00 AM, 14: 03:15 AM, 15: 03:30 AM, 16: 03:45 AM, 17: 04:00 AM, 18: 04:15 AM, 19: 04:30 AM, 20: 04:45 AM,
                    21: 05:00 AM, 22: 05:15 AM, 23: 05:30 AM, 24: 05:45 AM, 25: 06:00 AM, 26: 06:15 AM, 27: 06:30 AM, 28: 06:45 AM, 29: 07:00 AM, 30: 07:15 AM, 31: 07:30 AM, 32: 07:45 AM, 33: 08:00 AM, 34: 08:15 AM, 35: 08:30 AM, 36: 08:45 AM, 37: 09:00 AM, 38: 09:15 AM, 39: 09:30 AM,
                    40: 09:45 AM, 41: 10:00 AM, 42: 10:15 AM, 43: 10:30 AM, 44: 10:45 AM, 45: 11:00 AM, 46: 11:15 AM, 47: 11:30 AM, 48: 11:45 AM, 49: 12:00 PM, 50: 12:15 PM, 51: 12:30 PM, 52: 12:45 PM, 53: 01:00 PM, 54: 01:15 PM, 55: 01:30 PM, 56: 01:45 PM, 57: 02:00 PM, 58: 02:15 PM, 59: 02:30 PM, 60: 02:45 PM,
                    61: 03:00 PM, 62: 03:15 PM, 63: 03:30 PM, 64: 03:45 PM, 65: 04:00 PM, 66: 04:15 PM, 67: 04:30 PM, 68: 04:45 PM, 69: 05:00 PM, 70: 05:15 PM, 71: 05:30 PM, 72: 05:45 PM, 73: 06:00 PM, 74: 06:15 PM, 75: 06:30 PM, 76: 06:45 PM, 77: 07:00 PM, 78: 07:15 PM, 79: 07:30 PM, 80: 07:45 PM,
                    81: 08:00 PM, 82: 08:15 PM, 83: 08:30 PM, 84: 08:45 PM, 85: 09:00 PM, 86: 09:15 PM, 87: 09:30 PM, 88: 09:45 PM, 89: 10:00 PM, 90: 10:15 PM, 91: 10:30 PM, 92: 10:45 PM, 93: 11:00 PM, 94: 11:15 PM, 95: 11:30 PM, 96: 11:45 PM
                    Example: {value: 2:30 PM, valueEntityId: 59}.
                    """;
            case EMAIL, EMAILS -> "Email value as plain text. Example: {value: john@example.com}.";
            case PHONE, PHONES -> "Phone number as plain text. Example: {value: 1234567890}.";
            case MONETARY, DEAL_VALUE -> "Monetary amount as numeric string. Example: {value: 5000}.";
            case ADDRESS -> "Address text value. Example: {value: 123 Main St}.";
            case AUTO_NUMBER -> "Auto-number value as string. Example: {value: PRD-001}.";
            case TAGS ->
                    "Tag IDs. Call get_field_options with fieldName='tags'. Pass multiple value objects: [{valueEntityId: 1}, {valueEntityId: 2}].";
            case TERRITORY ->
                    "Call get_field_options with fieldName='territory'. Example: {value: name, valueEntityId: 123}";
            default -> "Plain text string value. Example: {value: text}";
        };
    }

    /**
     * Builds an example criteria object for the given field and store type.
     *
     * @param field       the field name
     * @param storeType   the type of the field
     * @param comparators the available comparators for the field
     * @return an example criteria object
     */
    private JsonObject buildCriteriaExample(final String field, final StoreType storeType,
                                            final Collection<Comparator> comparators) {
        final JsonObject example = Json.object();

        example.put("field", Json.object().put("fieldId", field));

        if (comparators != null && !comparators.isEmpty()) {
            example.put("comparatorId", comparators.iterator().next().getId());
        } else {
            example.put("comparatorId", 1);
        }

        final JsonArray valuesArray = Json.array();
        final JsonObject valueObj = Json.object();

        switch (storeType) {
            case SINGLE_STRING -> valueObj.put("value", "John");
            case SINGLE_NUMBER -> valueObj.put("value", "100");
            case DROPDOWN, CONTACT, COMPANY, CONTACT_ONBOARD, CONTACTS_ONBOARD, COMPANY_ONBOARD, OWNER -> {
                valueObj.put("value", "optionName");
                valueObj.put("valueEntityId", 123);
            }
            case MULTI_SELECT, TAGS -> {
                valueObj.put("value", "optionName");
                valueObj.put("valueEntityId", 1);
            }
            case DATE, DATE_TIME, DATE_RANGE, DATE_TIME_RANGE -> {
                valueObj.put("value", "Today");
                valueObj.put("valueEntityId", 23);
            }
            case TIME, TIME_RANGE -> {
                valueObj.put("value", "2:30 PM");
                valueObj.put("valueEntityId", 59);
            }
            case EMAIL, EMAILS -> valueObj.put("value", "john@example.com");
            case PHONE, PHONES -> valueObj.put("value", "1234567890");
            case MONETARY, DEAL_VALUE -> valueObj.put("value", "5000");
            case ADDRESS -> valueObj.put("value", "123 Main St");
            case AUTO_NUMBER -> valueObj.put("value", "PRD-001");
            case TERRITORY -> {
                valueObj.put("value", "name");
                valueObj.put("valueEntityId", 123);
            }
            default -> valueObj.put("value", "exampleValue");
        }
        valuesArray.put(valueObj);

        example.put("values", valuesArray);
        example.put("connectorId", 1);

        return example;
    }

    /**
     * Returns documentation about the overall criteria structure.
     */
    private JsonObject getCriteriaStructureDoc() {
        final JsonObject doc = Json.object();

        doc.put("description", "Each criterion in the 'criteria' array filters the list by one field condition.");
        doc.put("field", "Field ID (from get_list_schema). Required.");
        doc.put("comparatorId", "Comparator/operator ID (from get_list_schema field entry). Required.");
        doc.put("values", "Array of value objects. Each has 'value' (display/text) and optionally 'valueEntityId' for date fields (from dateIntervals in get_list_schema). Not required when comparator needsValue=false (e.g., 'is empty', 'is not empty').");
        doc.put("connectorId", "Logical connector to the NEXT criterion: 1=AND, 2=OR. Optional, defaults to AND.");
        doc.put("note", "Connectors chain criteria: criterion1 AND/OR criterion2 AND/OR criterion3...");

        return doc;
    }
}
