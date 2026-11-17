package com.tlc.crm.zapier.internal.action.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.json.JsonArray;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.zapier.api.models.ZapAction;
import com.tlc.crm.zapier.api.models.ZapierField;
import com.tlc.crm.zapier.fields.ZapAttributes;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;

/**
 * Implementation of {@link TagZapActionJsonFormatter} for formatting
 * tag-related data into a JSON structure suitable for Zapier actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TagZapActionJsonFormatter extends AbstractZapActionJsonFormatter {

    private static final class Instance {
        private static final TagZapActionJsonFormatter ZAP_ACTION_JSON_FORMATTER = new TagZapActionJsonFormatter();
    }

    public static TagZapActionJsonFormatter getInstance() {
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
        final JsonObject tagJson = Json.object();
        final Collection<ZapierField> fields = zapierFieldService.getFieldsByActionId(zapAction.getId());

        for (final ZapierField zapierField : fields) {
            final String aliasName = zapierField.getAliasName();

            switch (aliasName) {
                case "tag" -> {
                    final JsonArray tags = Json.array();

                    actionJson.getJsonArray(aliasName).forEach(value -> {
                        final JsonObject jsonObject = Json.object();

                        jsonObject.put(ZapAttributes.ID, NumberUtils.parseNumber(value).longValue());
                        tags.put(jsonObject);
                    });
                    tagJson.put(ZapAttributes.TAGS, tags);
                }
                case "contact" -> {
                    tagJson.put(ZapAttributes.ENTITY_ID, actionJson.getLong(aliasName));
                    tagJson.put(ZapAttributes.SOURCE, Source.CONTACT.getName());
                }
                case "deal" -> {
                    tagJson.put(ZapAttributes.ENTITY_ID, actionJson.getLong(aliasName));
                    tagJson.put(ZapAttributes.SOURCE, Source.DEAL.getName());
                }
                case "company" -> {
                    tagJson.put(ZapAttributes.ENTITY_ID, actionJson.getLong(aliasName));
                    tagJson.put(ZapAttributes.SOURCE, Source.COMPANY.getName());
                }
            }
        }

        return tagJson;
    }
}
