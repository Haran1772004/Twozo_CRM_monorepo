package com.tlc.social.server.whatsapp.action.event.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.social.common.whatsapp.events.WAEvents;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.internal.status.WAErrorCodes;
import com.tlc.social.common.whatsapp.model.WABusinessAccount;
import com.tlc.social.server.whatsapp.internal.service.WATemplateSyncPublisher;
import com.tlc.social.server.whatsapp.manager.WABusinessManager;

import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

/**
 * <p>
 * A handler which initiates the WhatsApp template sync process.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = WAEvents.SYNC_WA_TEMPLATES)
public class WATemplateSyncInitiateHandler implements EventConsumer<byte[]> {

    private final WABusinessManager waBusinessManager;
    private final WATemplateSyncPublisher waTemplateSyncPublisher;

    public WATemplateSyncInitiateHandler() {
        waBusinessManager = WABusinessManager.getInstance();
        waTemplateSyncPublisher = WATemplateSyncPublisher.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param bytes Represents the data to be consumed
     * @return the response in bytes
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject response = Json.object();
        final Long businessAccountId = Json.object(bytes).getLong(WAFields.BUSINESS_ACCOUNT_ID);

        waBusinessManager.getBusinessAccountById(businessAccountId).ifPresentOrElse(waBusinessAccount -> {
            if (WABusinessAccount.TemplateSyncStatus.IN_PROGRESS == waBusinessAccount.getSyncStatus()) {
                response.put(WAFields.STATUS, "i18n.whatsapp.template.sync.already.in.progress");
            } else {
                waTemplateSyncPublisher.publish(waBusinessAccount);
                response.put(WAFields.STATUS, "i18n.whatsapp.template.sync.initiated");
            }
        }, () -> {
            throw ErrorCode.get(WAErrorCodes.WA_BUSINESS_ACCOUNT_NOT_FOUND, "i18n.social.whatsapp.business.account.not.found");
        });

        return response.getBytes();
    }
}