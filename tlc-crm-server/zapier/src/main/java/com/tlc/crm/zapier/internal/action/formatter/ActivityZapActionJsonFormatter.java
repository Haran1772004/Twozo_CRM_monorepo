package com.tlc.crm.zapier.internal.action.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.zapier.api.models.ZapAction;
import com.tlc.crm.zapier.fields.ZapAttributes;

import com.tlc.l10n.temporal.TemporalServiceProvider;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;

/**
 * Implementation of {@link ActivityZapActionJsonFormatter} for formatting
 * activity-related data into a JSON structure suitable for Zapier actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ActivityZapActionJsonFormatter extends AbstractZapActionJsonFormatter {

    private static final class Instance {
        private static final ActivityZapActionJsonFormatter ZAP_ACTION_JSON_FORMATTER = new ActivityZapActionJsonFormatter();
    }

    public static ActivityZapActionJsonFormatter getInstance() {
        return Instance.ZAP_ACTION_JSON_FORMATTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      The ID of the organization, used to apply organization-specific logic or formatting.
     * @param zapAction  The Zapier action containing data to be formatted.
     * @param actionJson The existing JSON object that will be populated with the formatted action data.
     * @return The formatted JSON object ready for sending as a Zapier action payload.
     */
    @Override
    public JsonObject format(final long orgId, final ZapAction zapAction, final JsonObject actionJson) {
        final JsonObject activityJson = Json.object();
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, Source.ACTIVITY.getName());

        for (final FieldInfo fieldInfo : fields) {
            final String aliasName = fieldInfo.aliasName();

            switch (aliasName) {
                case "title", "description", "location" -> {
                    if (actionJson.containsKey(aliasName)) {
                        activityJson.put(aliasName, actionJson.getString(aliasName));
                    }
                }
                case "note" -> {
                    if (actionJson.containsKey(aliasName)) {
                        activityJson.put(aliasName, Json.object().put(ZapAttributes.CONTENT, actionJson.getString(aliasName)));
                    }
                }
                case "status", "dealId", "companyId", "typeId", "assigneeId",
                     "availability", "reminder", "priority" -> {
                    if (actionJson.containsKey(aliasName)) {
                        activityJson.put(aliasName, actionJson.getLong(aliasName));
                    }
                }
                case "id" -> {
                    if (actionJson.containsKey(aliasName)) {
                        activityJson.put(ZapAttributes.ID, actionJson.getLong(aliasName));
                    }
                }
                case "startTime", "endTime" -> {
                    if (actionJson.containsKey(aliasName)) {
                        activityJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetTimeStamp(actionJson.getString(aliasName)));
                    }
                }
                case "contactIds", "collaborators" -> {
                    if (actionJson.containsKey(aliasName)) {
                        final JsonArray array = Json.array();

                        actionJson.getJsonArray(aliasName).forEach(value -> array.put(NumberUtils.parseNumber(value).longValue()));
                        activityJson.put(aliasName, array);
                    }
                }
            }
        }
        return actionJson;
    }
}
