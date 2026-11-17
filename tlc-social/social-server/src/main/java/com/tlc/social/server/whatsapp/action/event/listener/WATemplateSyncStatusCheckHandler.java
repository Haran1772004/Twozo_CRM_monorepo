package com.tlc.social.server.whatsapp.action.event.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.social.common.whatsapp.events.WAEvents;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.internal.status.WAErrorCodes;
import com.tlc.social.common.whatsapp.model.WABusinessAccount;
import com.tlc.social.server.whatsapp.manager.WABusinessManager;

import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

/**
 * A handler which listens to the WhatsApp template sync status check event
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = WAEvents.CHECK_WA_TEMPLATE_SYNC_STATUS)
public class WATemplateSyncStatusCheckHandler implements EventConsumer<byte[]> {

    private final WABusinessManager waBusinessManager;

    public WATemplateSyncStatusCheckHandler() {
        waBusinessManager = WABusinessManager.getInstance();
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
            final WABusinessAccount.TemplateSyncStatus syncStatus = waBusinessAccount.getSyncStatus();

            response.put(WAFields.STATUS, syncStatus.getId());

            if (syncStatus.isCompleted()) {
                response.put(WAFields.LAST_SYNCED_TIME, waBusinessAccount.getLastSyncedTime().getUtcTimeStamp());
            }
        }, () -> {
            response.put(WAFields.ERRORS, "i18n.social.whatsapp.business.account.not.found");
        });

        return response.getBytes();
    }
}