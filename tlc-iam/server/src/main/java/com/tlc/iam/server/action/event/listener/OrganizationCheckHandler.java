package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.OrganizationFields;
import com.tlc.iam.server.internal.api.manager.OrganizationManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

/**
 * A listener that listens for events that is used check whether an organization exists or not
 *
 * @author Selvakumar
 * @version 1.0
 */
@EventListener(name = IamEvents.CHECK_ORG_EXISTENCE)
public class OrganizationCheckHandler implements EventConsumer<byte[]> {

    @Override
    public Boolean consume(final byte[] organizationBytes) {
        final JsonObject jsonObject = Json.object(organizationBytes);
        final OrganizationManager organizationManager = OrganizationManager.getInstance();

        if (jsonObject.containsKey(OrganizationFields.ID)) {
            return organizationManager.isExists(jsonObject.getLong(OrganizationFields.ID));
        } else if (jsonObject.containsKey(OrganizationFields.NAME)) {
            return organizationManager.isExists(jsonObject.getString(OrganizationFields.NAME));
        }

        return false;
    }
}
