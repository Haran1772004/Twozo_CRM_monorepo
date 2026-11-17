package com.tlc.crm.zapier.converter;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.zapier.api.manager.ZapEventWebhookManager;
import com.tlc.crm.zapier.fields.ZapAttributes;
import com.tlc.crm.zapier.api.models.ZapEvent;
import com.tlc.crm.zapier.api.models.ZapEventWebhook;

/**
 * Provides different ways to convert the data from / into {@link ZapEventWebhook}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class ZapEventWebhookConverter implements ZapConverter<ZapEventWebhook> {

    private final ZapEventWebhookManager zapTriggerWebhookManager;

    private ZapEventWebhookConverter() {
        this.zapTriggerWebhookManager = ZapEventWebhookManager.getInstance();
    }

    private static class Instance {
        private static final ZapEventWebhookConverter WEBHOOK_CONVERTER = new ZapEventWebhookConverter();
    }

    /**
     * Gets the instance of {@code ZapTriggerWebhookConverter}.
     *
     * @return The {@code ZapTriggerWebhookConverter} instance.
     */
    public static ZapEventWebhookConverter getInstance() {
        return Instance.WEBHOOK_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig The configuration containing conversion settings.
     * @param jsonObject      The JSON object to be converted.
     * @return The converted object of type {@code ZapTriggerWebhook}.
     */
    @Override
    public ZapEventWebhook convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Long orgId = converterConfig.orgId();
        final ZapEventWebhook zapEventWebhook = converterConfig.isPartialData()
                ? zapTriggerWebhookManager.get(orgId, jsonObject.getLong(ZapAttributes.ID)) : new ZapEventWebhook();

        if (jsonObject.containsKey(ZapAttributes.ID)) {
            zapEventWebhook.setId(jsonObject.getLong(ZapAttributes.ID));
        }
        zapEventWebhook.setOrgId(orgId);
        zapEventWebhook.setUserId(converterConfig.ownerId());
        zapEventWebhook.setZapId(jsonObject.containsKey(ZapAttributes.ZAP_ID)
                ? jsonObject.getString(ZapAttributes.ZAP_ID) : zapEventWebhook.getZapId());
        zapEventWebhook.setUrl(jsonObject.containsKey(ZapAttributes.HOOK_URL)
                ? jsonObject.getString(ZapAttributes.HOOK_URL) : zapEventWebhook.getUrl());
        zapEventWebhook.setSubscribed(jsonObject.optBoolean(ZapAttributes.IS_SUBSCRIBED));
        zapEventWebhook.setZapEvent(ZapEvent.get(jsonObject.getShort(ZapAttributes.EVENT)));

        return zapEventWebhook;
    }
}
