package com.tlc.crm.zapier.internal.action.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.zapier.api.models.ZapAction;
import com.tlc.crm.zapier.api.models.ZapierField;
import com.tlc.crm.zapier.fields.ZapAttributes;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;

/**
 * Implementation of {@link FollowerZapActionJsonDispatcher} for formatting
 * follower-related data into a JSON structure suitable for Zapier actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FollowerZapActionJsonDispatcher extends AbstractZapActionJsonFormatter {

    private static final class Instance {
        private static final FollowerZapActionJsonDispatcher ZAP_ACTION_JSON_DISPATCHER = new FollowerZapActionJsonDispatcher();
    }

    public static FollowerZapActionJsonDispatcher getInstance() {
        return Instance.ZAP_ACTION_JSON_DISPATCHER;
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
        final JsonObject followerJson = Json.object();
        final Collection<ZapierField> fields = zapierFieldService.getFieldsByActionId(zapAction.getId());

        for (final ZapierField zapierField : fields) {
            final String aliasName = zapierField.getAliasName();

            switch (aliasName) {
                case "follower" -> {
                    final JsonArray followerIds = Json.array();

                    actionJson.getJsonArray(aliasName).forEach(value -> followerIds.put(NumberUtils.parseNumber(value).longValue()));
                    followerJson.put(ZapAttributes.FOLLOWER_IDS, followerIds);
                }
                case "contact" -> {
                    followerJson.put(ZapAttributes.ENTITY_ID, actionJson.getLong(aliasName));
                    followerJson.put(ZapAttributes.SOURCE, Source.CONTACT.getName());
                }
                case "deal" -> {
                    followerJson.put(ZapAttributes.ENTITY_ID, actionJson.getLong(aliasName));
                    followerJson.put(ZapAttributes.SOURCE, Source.DEAL.getName());
                }
                case "company" -> {
                    followerJson.put(ZapAttributes.ENTITY_ID, actionJson.getLong(aliasName));
                    followerJson.put(ZapAttributes.SOURCE, Source.COMPANY.getName());
                }
            }
        }

        return followerJson;
    }
}
