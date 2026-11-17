package com.tlc.crm.zapier.internal.action.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.zapier.api.models.ZapAction;
import com.tlc.crm.zapier.api.models.ZapierField;
import com.tlc.crm.zapier.fields.ZapAttributes;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;

/**
 * Implementation of {@link NoteZapActionJsonFormatter} for formatting
 * note-related data into a JSON structure suitable for Zapier actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoteZapActionJsonFormatter extends AbstractZapActionJsonFormatter {

    private static final class Instance {
        private static final NoteZapActionJsonFormatter ZAP_ACTION_JSON_FORMATTER = new NoteZapActionJsonFormatter();
    }

    public static NoteZapActionJsonFormatter getInstance() {
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
        final JsonObject noteJson = Json.object();
        final Collection<ZapierField> fields = zapierFieldService.getFieldsByActionId(zapAction.getId());

        for (final ZapierField zapierField : fields) {
            final String aliasName = zapierField.getAliasName();

            switch (aliasName) {
                case "note" -> noteJson.put(ZapAttributes.CONTENT, actionJson.getString(aliasName));
                case "contact" -> {
                    noteJson.put(ZapAttributes.ENTITY_ID, actionJson.getLong(aliasName));
                    noteJson.put(ZapAttributes.SOURCE, Source.CONTACT.getName());
                }
                case "deal" -> {
                    noteJson.put(ZapAttributes.ENTITY_ID, actionJson.getLong(aliasName));
                    noteJson.put(ZapAttributes.SOURCE, Source.DEAL.getName());
                }
                case "company" -> {
                    noteJson.put(ZapAttributes.ENTITY_ID, actionJson.getLong(aliasName));
                    noteJson.put(ZapAttributes.SOURCE, Source.COMPANY.getName());
                }
            }
        }

        return noteJson;
    }
}
