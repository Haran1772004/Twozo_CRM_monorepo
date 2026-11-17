package com.tlc.crm.entity.fieldaudit.action;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmSecureAction;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.common.models.SortType;
import com.tlc.crm.common.resolver.I18NCRMResolutionAdapter;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.entity.fieldaudit.FieldAuditHandler;
import com.tlc.crm.entity.fieldaudit.internal.status.FieldAuditErrorCodes;
import com.tlc.crm.entity.fieldaudit.keys.FieldAuditKeys;
import com.tlc.crm.entity.fieldaudit.manager.FieldAuditManager;
import com.tlc.crm.entity.fieldaudit.manager.FieldTrackingConfigManager;
import com.tlc.crm.entity.fieldaudit.models.DateIntervals;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditEntry;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditColumns;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditSource;
import com.tlc.crm.entity.fieldaudit.models.FieldTrackingConfig;
import com.tlc.crm.entity.fieldaudit.models.AuditFilter;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.internal.SecureFieldService;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * REST endpoint for Field Audit feature.
 *
 * @author Muthu kumar v
 * @version 1.0
 */
@WebAction(path = "/field/audit/mgmt")
public final class FieldAuditAction extends CrmSecureAction {

    private final FieldTrackingConfigManager trackingConfigManager;
    private final FieldAuditManager historyManager;
    private final FieldService fieldService;
    private final UserService userService;

    public FieldAuditAction() {
        this.trackingConfigManager = FieldTrackingConfigManager.getInstance();
        this.historyManager = FieldAuditManager.getInstance();
        this.fieldService = SecureFieldService.getInstance();
        this.userService = UserService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the request
     * @param response Represents the response
     */
    @Override
    public void sProcess(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(FieldAuditKeys.TYPE);
        final long orgId = request.orgId();
        final long userId = request.userId();
        final JsonObject data = requestJson.optJsonObject(FieldAuditKeys.DATA);

        switch (type) {
            case FieldAuditKeys.GET_FIELDS -> handleGetFields(orgId, data, response);
            case FieldAuditKeys.UPDATE_FIELDS -> handleUpdateFields(orgId, userId, data, response);
            case FieldAuditKeys.GET_FIELD_AUDIT -> handleGetFieldAudit(orgId, data, request.getI18NCRMResolutionAdapter(), response);
            case FieldAuditKeys.GET_TRACKING_ENABLED_FIELDS -> handleGetTrackingEnabledFields(orgId, data, response);
            case FieldAuditKeys.GET_TRACKING_DATE_INTERVAL_LIST -> handleGetTrackingDateIntervalList(response);
            case FieldAuditKeys.GET_TRACKING_SOURCE_LIST -> handleGetTrackingSourceList(data, response);
            default -> throw ErrorCode.get(FieldAuditErrorCodes.INVALID_REQUEST);
        }
    }

    /**
     * <p>
     * Gets the fields that are enabled for field audit tracking
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param data     Represents the request data
     * @param response Represents the response
     */
    private void handleGetFields(final long orgId, final JsonObject data, final CrmResponse response) {
        final Source source = Source.get(data.getString(FieldAuditKeys.SOURCE));
        final FieldTrackingConfig config = trackingConfigManager.getOrDefault(orgId, (short) source.getId());
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, source.getName()).stream().sorted(Comparator.comparing(FieldInfo::id)).toList();
        final JsonArray fieldsArray = Json.array();

        for (final FieldInfo fieldInfo : fields) {

            if (!FieldAuditHandler.isFieldAuditTrackable(fieldInfo)) {
                continue;
            }
            final String aliasName = fieldInfo.aliasName();
            final JsonObject fieldJson = Json.object();

            fieldJson.put(FieldAuditKeys.ID, fieldInfo.id());
            fieldJson.put(FieldAuditKeys.FIELD_NAME, aliasName);
            fieldJson.put(FieldAuditKeys.IS_TRACKING_ENABLED, config.isFieldTracked(aliasName));
            fieldJson.put(FieldAuditKeys.TYPE, fieldInfo.storeType().name());
            fieldJson.put(FieldAuditKeys.DISPLAY_NAME, response.resolveI18nPlaceholder(fieldInfo.i18nName()));
            fieldsArray.put(fieldJson);
        }
        response.put(FieldAuditKeys.FIELDS, fieldsArray);
    }

    /**
     * <p>
     * Updates the tracking enabled fields
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param userId   Represents the user id
     * @param data     Represents the request data
     * @param response Represents the response
     */
    private void handleUpdateFields(final long orgId, final long userId, final JsonObject data, final CrmResponse response) {
        final Set<String> trackedFields = new LinkedHashSet<>();

        if (data.containsKey(FieldAuditKeys.FIELDS)) {
            final JsonArray fieldsArray = data.getJsonArray(FieldAuditKeys.FIELDS);
            final Map<String, FieldInfo> fieldInfoMap = fieldService.groupDefaultFieldsByAliasName(orgId, data.getString(FieldAuditKeys.SOURCE));

            for (int index = 0; index < fieldsArray.size(); index++) {
                final JsonObject fieldJson = fieldsArray.getJsonObject(index);

                if (fieldJson.optBoolean(FieldAuditKeys.IS_TRACKING_ENABLED, false)) {
                    final String fieldName = fieldJson.getString(FieldAuditKeys.FIELD_NAME);

                    if (fieldInfoMap.containsKey(fieldName)) {
                        trackedFields.add(fieldName);
                    }
                }
            }
        }
        trackingConfigManager.update(orgId, userId, (short) Source.get(data.getString(FieldAuditKeys.SOURCE)).getId(), trackedFields);

        response.put(FieldAuditKeys.MESSAGE, response.resolveI18nString("i18n.field.audit.update.success"));
    }

    /**
     * <p>
     * Gets the field audit list
     * </p>
     *
     * @param orgId     Represents the organization id
     * @param data      Represents the request data
     * @param response  Represents the response
     */
    private void handleGetFieldAudit(final long orgId, final JsonObject data, final I18NCRMResolutionAdapter i18NCRMResolutionAdapter, final CrmResponse response) {
        final Source source = Source.get(data.getString(FieldAuditKeys.SOURCE));
        final long entityId = data.getLong(FieldAuditKeys.ID);
        final AuditFilter.Builder filterBuilder = AuditFilter.builder().sourceId((short) source.getId()).entityId(entityId);

        loadColumns(response);

        if (data.containsKey(FieldAuditKeys.CRITERIA)) {
            applyCriteria(data.getJsonObject(FieldAuditKeys.CRITERIA), filterBuilder);
        }

        if (data.containsKey(FieldAuditKeys.START)) {
            filterBuilder.start(data.getInt(FieldAuditKeys.START));
        }

        if (data.containsKey(FieldAuditKeys.LIMIT)) {
            filterBuilder.size(data.getInt(FieldAuditKeys.LIMIT));
        }

        if (data.containsKey(FieldAuditKeys.SORT)) {
            final JsonObject sortJson = data.getJsonObject(FieldAuditKeys.SORT);
            final String sortField = sortJson.getString(FieldAuditKeys.FIELD);
            final String sort = sortJson.getString(FieldAuditKeys.TYPE);

            if (FieldAuditKeys.UPDATED_ON.equals(sortField)) {
                filterBuilder.sortField(sortField);
                filterBuilder.sortType(SortType.ASCENDING == SortType.getSortType(sort) ? OrderByClause.OrderType.ASCENDING : OrderByClause.OrderType.DESCENDING);
            }
        }
        final AuditFilter auditFilter = filterBuilder.build();
        final long totalCount = historyManager.fetchCount(orgId, auditFilter);

        if (totalCount > 0) {
            final Map<Long, FieldInfo> fieldMap = fieldService.getFieldsBySource(orgId, source.getName()).stream()
                    .filter(FieldInfo::isActive).collect(Collectors.toMap(FieldInfo::id, Function.identity(), (a, b) -> a));
            final Collection<FieldAuditEntry> historyRows = historyManager.fetchRecords(orgId, auditFilter);
            final Set<Long> userIds = historyRows.stream().map(FieldAuditEntry::updatedBy).filter(Objects::nonNull).collect(Collectors.toSet());
            final Map<Long, String> userNameMap = resolveUserNames(orgId, userIds);
            final JsonArray historyArray = Json.array();

            for (final FieldAuditEntry history : historyRows) {
                final FieldInfo fieldInfo = fieldMap.get(history.getFieldId());

                if (Objects.isNull(fieldInfo)) {
                    continue;
                }
                final JsonObject jsonObject = Json.object();
                final StoreType storeType = fieldInfo.storeType();
                final String userName = Objects.nonNull(history.updatedBy()) ? userNameMap.getOrDefault(history.updatedBy(), String.valueOf(history.updatedBy())) : FieldAuditKeys.PLACEHOLDER;

                jsonObject.put(FieldAuditKeys.ID, history.id());
                jsonObject.put(FieldAuditKeys.FIELD_NAME, Json.object().put(FieldAuditKeys.VALUE, response.resolveI18nPlaceholder(fieldInfo.i18nName())));
                jsonObject.put(FieldAuditKeys.PREVIOUS_VALUE, buildValueJson(orgId, history.previousValue(), storeType, i18NCRMResolutionAdapter));
                jsonObject.put(FieldAuditKeys.CHANGED_VALUE, buildValueJson(orgId, history.newValue(), storeType, i18NCRMResolutionAdapter));
                jsonObject.put(FieldAuditKeys.UPDATED_ON, buildValueJson(orgId, String.valueOf(history.updatedAt()), StoreType.DATE_TIME, null));
                jsonObject.put(FieldAuditKeys.UPDATED_BY, buildValueJson(orgId, userName, StoreType.SINGLE_STRING, null));
                jsonObject.put(FieldAuditKeys.FIELD_TYPE, storeType.getName());
                final FieldAuditSource fieldAuditSource = history.auditSource();
                final String subvalue = fieldAuditSource.getSubvalue();
                final Long sourceReferenceId = history.auditSourceReferenceId();

                final JsonObject updateViaJson = Json.object()
                        .put(FieldAuditKeys.TYPE, fieldAuditSource.name())
                        .put(FieldAuditKeys.VALUE, response.resolveI18nPlaceholder(fieldAuditSource.getDisplayName()));

                if (Objects.nonNull(subvalue)) {
                    updateViaJson.put(FieldAuditKeys.SUB_VALUE, response.resolveI18nPlaceholder(subvalue));
                } else if (fieldAuditSource.hasDynamicSubvalue() && Objects.nonNull(sourceReferenceId)) {
                    final JsonObject metadata = history.metadata();

                    updateViaJson.put(FieldAuditKeys.SUB_VALUE, metadata.opt(FieldAuditKeys.NAME));
                    updateViaJson.put(FieldAuditKeys.IS_ACTIVE, metadata.optBoolean(FieldAuditKeys.IS_ACTIVE, true));
                }

                if (Objects.nonNull(sourceReferenceId)) {
                    updateViaJson.put(FieldAuditKeys.ID, sourceReferenceId);
                }
                jsonObject.put(FieldAuditKeys.UPDATED_VIA, updateViaJson);
                historyArray.put(jsonObject);
            }
            response.put(FieldAuditKeys.ROWS, historyArray);
        }
        response.put(FieldAuditKeys.TOTAL, totalCount);
        response.put(FieldAuditKeys.START, auditFilter.start());
        response.put(FieldAuditKeys.LIMIT, auditFilter.size());
        response.put(FieldAuditKeys.HAS_MORE, (auditFilter.start() + auditFilter.size()) <= totalCount);
    }

    /**
     * <p>
     * Loads the column data required for the edit history list
     * </p>
     *
     * @param response Represents the response
     */
    private void loadColumns(final CrmResponse response) {
        final JsonArray columns = Json.array();

         for (final FieldAuditColumns column : FieldAuditColumns.values()) {
             final JsonObject jsonObject = Json.object();

             jsonObject.put(FieldAuditKeys.KEY, column.getColumnName());
             jsonObject.put(FieldAuditKeys.LABEL, response.resolveI18nString(column.getDisplayName()));
             jsonObject.put(FieldAuditKeys.TYPE, column.getType());
             jsonObject.put(FieldAuditKeys.IS_SORT_ENABLED, column.isSortEnabled());
             columns.put(jsonObject);
         }
        response.put(FieldAuditKeys.COLUMNS, columns);
    }

    /**
     * <p>
     * Gets the tracking enabled fields
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param data     Represents the request data
     * @param response Represents the response
     */
    private void handleGetTrackingEnabledFields(final long orgId, final JsonObject data, final CrmResponse response) {
        final Source source = Source.get(data.getString(FieldAuditKeys.SOURCE));
        final FieldTrackingConfig fieldTrackingConfig = trackingConfigManager.getOrDefault(orgId, (short) source.getId());
        final JsonArray fieldsArray = Json.array();
        final Map<String, FieldInfo> fieldInfoMap = fieldService.groupDefaultFieldsByAliasName(orgId, source.getName());

        fieldTrackingConfig.trackedFields().stream().filter(fieldInfoMap::containsKey).map(fieldInfoMap::get)
                .sorted(Comparator.comparing(FieldInfo::id)).forEach(field -> {
            final String aliasName = field.aliasName();
            final JsonObject fieldJson = Json.object();

            fieldJson.put(FieldAuditKeys.ID, field.id());
            fieldJson.put(FieldAuditKeys.VALUE, aliasName);
            fieldJson.put(FieldAuditKeys.LABEL, response.resolveI18nPlaceholder(field.i18nName()));
            fieldJson.put(FieldAuditKeys.TYPE, field.storeType().name());
            fieldsArray.put(fieldJson);
        });
        response.put(FieldAuditKeys.FIELDS, fieldsArray);
    }

    /**
     * <p>
     * Gets the tracking date intervallist
     * </p>
     *
     * @param response Represents the response
     */
    private void handleGetTrackingDateIntervalList(final CrmResponse response) {
        final JsonArray intervals = Json.array();

        for (final DateIntervals dateInterval : DateIntervals.values()) {
            final JsonObject interval = Json.object();

            interval.put(FieldAuditKeys.ID, dateInterval.getId());
            interval.put(FieldAuditKeys.TYPE, response.resolveI18nString(dateInterval.getDisplayName()));
            intervals.put(interval);
        }
        response.put(FieldAuditKeys.DATE_INTERVALS, intervals);
    }

    /**
     * <p>
     * Gets the tracking source list
     * </p>
     *
     * @param data     Represents the request data
     * @param response Represents the response
     */
    private void handleGetTrackingSourceList(final JsonObject data, final CrmResponse response) {
        final Source source = Source.get(data.getString(FieldAuditKeys.SOURCE));
        final JsonArray sources = Json.array();

        for (final FieldAuditSource fieldAuditSource : FieldAuditSource.getFieldAuditSources((short) source.getId())) {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(FieldAuditKeys.ID, fieldAuditSource.getId());
            jsonObject.put(FieldAuditKeys.VALUE, fieldAuditSource.name());
            jsonObject.put(FieldAuditKeys.LABEL, response.resolveI18nPlaceholder(fieldAuditSource.getDisplayName()));
            sources.put(jsonObject);
        }
        response.put(FieldAuditKeys.SOURCES, sources);
    }

    /**
     * <p>
     * Applies the criteria from the request JSON object to the AuditFilter builder.
     * </p>
     *
     * @param criteria Represents the criteria JSON object containing filter conditions
     * @param builder  Represents the AuditFilter builder to which the criteria will be applied
     */
    private void applyCriteria(final JsonObject criteria, final AuditFilter.Builder builder) {
        if (criteria.containsKey(FieldAuditKeys.FIELDS)) {
            final JsonArray fieldsArray = criteria.getJsonArray(FieldAuditKeys.FIELDS);
            final Set<Long> fieldIds = new HashSet<>();

            for (int index = 0; index < fieldsArray.size(); index++) {
                fieldIds.add(fieldsArray.getLong(index));
            }
            builder.fieldIds(fieldIds);
        }

        if (criteria.containsKey(FieldAuditKeys.TIME_PERIODS)) {
            final JsonObject timePeriods = criteria.getJsonObject(FieldAuditKeys.TIME_PERIODS);

            if (timePeriods.containsKey(FieldAuditKeys.FROM)) {
                builder.fromTimeStamp(timePeriods.getLong(FieldAuditKeys.FROM));
            }

            if (timePeriods.containsKey(FieldAuditKeys.TO)) {
                builder.toTimestamp(timePeriods.getLong(FieldAuditKeys.TO));
            }
        }

        if (criteria.containsKey(FieldAuditKeys.USERS)) {
            final JsonArray usersArray = criteria.getJsonArray(FieldAuditKeys.USERS);
            final Set<Long> userIds = new HashSet<>();

            for (int index = 0; index < usersArray.size(); index++) {
                userIds.add(usersArray.getLong(index));
            }
            builder.userIds(userIds);
        }

        if (criteria.containsKey(FieldAuditKeys.SOURCES)) {
            final JsonArray criteriaJsonArray = criteria.getJsonArray(FieldAuditKeys.SOURCES);
            final Set<FieldAuditSource> sources = new HashSet<>();

            for (int index = 0; index < criteriaJsonArray.size(); index++) {
                sources.add(FieldAuditSource.get(criteriaJsonArray.getInt(index)));
            }
            builder.changeSources(sources);
        }
    }

    /**
     * <p>
     * Builds the value JSON object for a given field value, store type, and additional metadata such as count (for multi-value fields), sub-value, and an optional id.
     * </p>
     *
     * @param value     Represents the field value to be included in the JSON object. This can be a single value or a comma-separated string for multi-value fields.
     * @param storeType Represents the StoreType of the field, which determines how the value should be interpreted and displayed.
     * @param count     Represents the count of items for multi-value fields. This is used when the value is a comma-separated string to indicate how many individual values are present.
     * @return A JsonObject containing the field value, its type, and any additional metadata such as count, sub-value, and id. The structure of the JSON object is designed to provide a comprehensive representation of the field value for use in the field audit feature.
     */
    private JsonObject buildValueJson(final Long orgId, final Object value, final StoreType storeType, final Integer count, final I18NCRMResolutionAdapter i18NCRMResolutionAdapter) {
        final JsonObject jsonObject = Json.object();

        if (StoreType.OWNER == storeType) {
            final String stringValue = String.valueOf(value);

            if (Objects.nonNull(value) && !FieldAuditKeys.PLACEHOLDER.equals(stringValue)) {
                try {
                    jsonObject.put(FieldAuditKeys.VALUE, userService.getUser(orgId, Long.valueOf(stringValue)).getFullName());
                } catch (final NumberFormatException exception) {
                    jsonObject.put(FieldAuditKeys.VALUE, stringValue);
                }
            } else {
                jsonObject.put(FieldAuditKeys.VALUE, FieldAuditKeys.PLACEHOLDER);
            }
        } else if (StoreType.DATE_RANGE == storeType || StoreType.DATE_TIME_RANGE == storeType || StoreType.TIME_RANGE == storeType) {
            final JsonObject rangeJson = Json.object();

            if (value instanceof String[] arrayValue && arrayValue.length == 2) {
                rangeJson.put(FieldAuditKeys.FROM, NumberUtils.parseNumber(arrayValue[0]));
                rangeJson.put(FieldAuditKeys.TO, NumberUtils.parseNumber(arrayValue[1]));
                jsonObject.put(FieldAuditKeys.VALUE, rangeJson);
            } else {
                jsonObject.put(FieldAuditKeys.VALUE, FieldAuditKeys.PLACEHOLDER);
            }
        } else {

            if (value instanceof String stringValue) {

                if (FieldAuditKeys.PLACEHOLDER.equals(stringValue)) {
                    jsonObject.put(FieldAuditKeys.VALUE, stringValue);
                } else if (stringValue.startsWith("i18n")) {
                        jsonObject.put(FieldAuditKeys.VALUE, i18NCRMResolutionAdapter.get(stringValue));
                } else {

                    if (StoreType.DATE == storeType || StoreType.DATE_TIME == storeType || StoreType.TIME == storeType) {
                        jsonObject.put(FieldAuditKeys.VALUE, NumberUtils.parseNumber(stringValue));
                    } else {
                        jsonObject.put(FieldAuditKeys.VALUE, stringValue);
                    }
                }
            } else if (value instanceof String[] arrayValue) {

                for (int index = 0; index < arrayValue.length; index++) {
                    final String item = arrayValue[index];

                    if (item.startsWith("i18n")) {
                        arrayValue[index] = i18NCRMResolutionAdapter.get(item);
                    }
                }
                jsonObject.put(FieldAuditKeys.VALUE, arrayValue);

                if (Objects.nonNull(count)) {
                    jsonObject.put(FieldAuditKeys.COUNT, count);
                }
            }
        }
        return jsonObject;
    }

    /**
     * <p>
     * Builds the value json
     * </p>
     *
     * @param value     Represents the field value
     * @param storeType Represens the store type
     * @return A JsonObject containing the field value and its type
     */
    private JsonObject buildValueJson(final Long orgId, final String value, final StoreType storeType, final I18NCRMResolutionAdapter i18NCRMResolutionAdapter) {
        if (Objects.isNull(value) || value.isEmpty()) {
            return buildValueJson(orgId, FieldAuditKeys.PLACEHOLDER, storeType, null, i18NCRMResolutionAdapter);
        }

        if (value.contains(FieldAuditKeys.COMMA) && isMultiValueStoreType(storeType)) {
            final String[] parts = value.split(FieldAuditKeys.COMMA);

            return buildValueJson(orgId, parts, storeType, parts.length, i18NCRMResolutionAdapter);
        }
        return buildValueJson(orgId, value, storeType, null,i18NCRMResolutionAdapter);
    }

    /**
     * <p>
     * Checks if the store type represents a multi-value field
     * </p>
     *
     * @param storeType Represents the store type to check
     * @return true if the store type is a multi-value type, false otherwise
     */
    private boolean isMultiValueStoreType(final StoreType storeType) {
        return storeType == StoreType.MULTI_SELECT || storeType == StoreType.TAGS || storeType == StoreType.EMAILS
                || storeType == StoreType.PHONES || storeType == StoreType.DATE_RANGE
                || storeType == StoreType.DATE_TIME_RANGE || storeType == StoreType.TIME_RANGE || storeType == StoreType.MARKETING_LIST;
    }

    /**
     * <p>
     * Resolves the user names
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param userIds  Represents the set of user ids for which the names need to be resolved
     * @return A map of user ids to their corresponding full names.
     */
    private Map<Long, String> resolveUserNames(final long orgId, final Set<Long> userIds) {
        if (Objects.isNull(userIds) || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        final Collection<CrmUser> users = userService.getUsers(orgId, userIds);
        final Map<Long, String> nameMap = new HashMap<>();

        for (final CrmUser user : users) {
            nameMap.put(user.id(), user.getFullName());
        }
        return nameMap;
    }
}