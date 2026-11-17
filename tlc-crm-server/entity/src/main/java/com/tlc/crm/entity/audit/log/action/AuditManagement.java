package com.tlc.crm.entity.audit.log.action;

import com.tlc.commons.Counter;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.util.DateUtils;
import com.tlc.crm.entity.audit.log.model.AuditAction;
import com.tlc.crm.entity.audit.log.model.AuditAttribute;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;

import com.tlc.crm.entity.audit.log.fields.AuditEntryFields;
import com.tlc.crm.entity.audit.log.manager.AuditManager;
import com.tlc.crm.entity.audit.log.model.AuditEntity;
import com.tlc.crm.entity.audit.log.model.AuditLog;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.fields.FieldKeys;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.web.WebAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * <p>
 * Manages {@link AuditLog} related web actions
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
@WebAction(path = "/audit/mgmt")
public class AuditManagement extends AuthorizationController {

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 50;
    private static final String TIMESTAMP_KEY = "timeStamp";
    private final DateTimeUtil dateTimeUtil;
    private final UserService userService;

    public AuditManagement() {
        dateTimeUtil = DateTimeUtil.getInstance();
        this.userService = UserService.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse response) throws Exception {
        final Long orgId = request.orgId();
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(FieldKeys.DATA.getName());
        final String userIdKey = FieldKeys.USER_ID.getName();
        final Long userId = data.containsKey(userIdKey) ? data.getLong(userIdKey) : request.userId();

        switch (requestJson.getString(FieldKeys.TYPE.getName())) {
            case "getUserAudits" -> loadUserAudits(orgId, userId, data, request, response);
        }
    }

    /**
     * <p>
     * Loads user audit logs for a given organization and user.
     * </p>
     *
     * @param orgId    the ID of the organization.
     * @param userId   the ID of the user.
     * @param data     the JSON object containing start and limit values for pagination.
     * @param request  the CRM request object.
     * @param response the CRM response object.
     */
    private void loadUserAudits(final Long orgId, final Long userId, final JsonObject data, final CrmRequest request,
                                final CrmResponse response) {
        final int start = Math.max(data.optInt(ActivityFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(ActivityFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final AuditManager auditManager = AuditManager.getInstance();
        final long totalUpdatesCount = auditManager.getTotalLogsCount(orgId, userId);
        final Collection<AuditEntity> auditEntities = auditManager.getUserAudits(orgId, userId, start, limit);
        final String auditAttributeKey = AuditEntryFields.AUDIT_ATTRIBUTE.getName();
        final String fieldNameKey = AuditEntryFields.FIELD_NAME.getName();
        final String oldValueKey = AuditEntryFields.OLD_VALUE.getName();
        final String newValueKey = AuditEntryFields.NEW_VALUE.getName();
        final String subActionKey = AuditEntryFields.SUB_ACTION.getName();
        final String entityKey = AuditEntryFields.ENTITY.getName();
        final String actionTimeKey = AuditEntryFields.ACTION_TIME.getName();
        final String actionKey = AuditEntryFields.ACTION.getName();
        final JsonArray logsJson = Json.array();
        final Counter updatesCounter = new Counter();

        for (final AuditEntity auditEntity : auditEntities) {
            final Collection<AuditLog> auditedLogs = auditEntity.auditLogs();
            final JsonObject auditEntityJson = buildAuditEntityIntoJson(auditEntity);

            for (final AuditLog auditedLog : auditedLogs) {
                final Long actionTime = dateTimeUtil.getTimeStamp(auditedLog.actionTime());
                final AuditAction action = auditedLog.action();
                final Collection<AuditAttribute> auditAttributes = auditedLog.auditAttributes();
                final Consumer<JsonObject> detailsLoader = (logJson) -> {
                    logJson.put(actionTimeKey, actionTime);
                    logJson.put(actionKey, action);
                    logJson.put(entityKey, auditEntityJson);
                    logsJson.put(logJson);
                };

                updatesCounter.increment();

                if (Objects.isNull(auditAttributes) || auditAttributes.isEmpty()) {
                    detailsLoader.accept(Json.object());
                } else {
                    for (final AuditAttribute auditAttribute : auditAttributes) {
                        final JsonObject logJson = Json.object();
                        final JsonObject attributeJson = Json.object();
                        final Object oldValue = auditAttribute.oldValue();
                        final Object newValue = auditAttribute.newValue();

                        switch (auditAttribute.storeType()) {
                            case DATE -> {
                                attributeJson.put(oldValueKey, oldValue instanceof Map<?, ?> date ? DateUtils.getFormattedDateString((long) date.get(TIMESTAMP_KEY)) : oldValue);
                                attributeJson.put(newValueKey, newValue instanceof Map<?, ?> date ? DateUtils.getFormattedDateString((long) date.get(TIMESTAMP_KEY)) : newValue);
                            }
                            default -> {
                                attributeJson.put(oldValueKey, oldValue);
                                attributeJson.put(newValueKey, newValue);
                            }
                        }

                        attributeJson.put(fieldNameKey, response.resolveI18nPlaceholder(auditAttribute.name()));
                        attributeJson.put(subActionKey, auditAttribute.subAction());
                        logJson.put(auditAttributeKey, attributeJson);
                        detailsLoader.accept(logJson);
                    }
                }
            }
        }
        response.put(FieldKeys.LIST.getName(), groupAuditLogsByDate(logsJson, request));
        response.put(AuditEntryFields.TOTAL_UPDATE_COUNT.getName(), totalUpdatesCount);
        response.put(AuditEntryFields.LOGS_COUNT.getName(), logsJson.size());
        response.put(AuditEntryFields.UPDATES_COUNT.getName(), updatesCounter.getCount());
        response.put(FieldKeys.HAS_MORE.getName(), (start + limit) < totalUpdatesCount);
    }

    /**
     * <p>
     * Groups the audit log json array based on the date audit action time
     * </p>
     *
     * @param userAudits Represents the {@link JsonArray} details
     * @return The grouped audit log {@link JsonArray} details
     */
    private JsonArray groupAuditLogsByDate(final JsonArray userAudits, final CrmRequest crmRequest) {
        final Map<Long, List<JsonObject>> groupedAuditLogs = new TreeMap<>(Comparator.reverseOrder());
        final JsonArray jsonArray = Json.array();
        final String actionTimeKey = AuditEntryFields.ACTION_TIME.getName();
        final TimezoneInfo timezoneInfo = crmRequest.timeZone();
        final Locale locale = crmRequest.locale();
        final int size = userAudits.size();

        for (int index = 0; index < size; index++) {
            final JsonObject auditLog = userAudits.getJsonObject(index);
            final long actionTime = auditLog.getLong(actionTimeKey);
            final long startOfTheDay = TemporalServiceProvider.get(actionTime).getStartTimeOfDay(timezoneInfo);

            groupedAuditLogs.computeIfAbsent(startOfTheDay, dateGroup -> new ArrayList<>()).add(auditLog);
        }

        for (final Map.Entry<Long, List<JsonObject>> auditEntries : groupedAuditLogs.entrySet()) {
            final List<JsonObject> audits = auditEntries.getValue();
            final JsonArray auditLogs = Json.array();

            audits.sort((firstAudit, secondAudit) -> Long.compare(secondAudit.getLong(actionTimeKey), firstAudit.getLong(actionTimeKey)));
            audits.forEach(auditLogs::put);

            final JsonObject auditJson = Json.object();

            auditJson.put(AuditEntryFields.DATE.getName(), TemporalServiceProvider.get(auditEntries.getKey()).getLocalDate(timezoneInfo, locale));
            auditJson.put(AuditEntryFields.DATA.getName(), auditLogs);
            jsonArray.put(auditJson);
        }

        return jsonArray;
    }

    /**
     * <p>
     * Builds the {@link AuditEntity} details into {@link JsonObject}
     * </p>
     *
     * @param auditEntity Represents the {@link AuditEntity} details
     * @return The {@link JsonObject} details
     */
    private JsonObject buildAuditEntityIntoJson(final AuditEntity auditEntity) {
        final JsonObject entityJson = Json.object();

        entityJson.put(AuditEntryFields.TYPE.getName(), auditEntity.auditCategory().getName());
        entityJson.put(AuditEntryFields.ID.getName(), auditEntity.id());
        entityJson.put(AuditEntryFields.NAME.getName(), auditEntity.name());
        entityJson.put(AuditEntryFields.ICON_NAME.getName(), auditEntity.auditCategory().getIconName());

        return entityJson;
    }
}
