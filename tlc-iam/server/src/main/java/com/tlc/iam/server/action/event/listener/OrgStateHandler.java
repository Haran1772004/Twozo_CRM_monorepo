package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.OrganizationFields;
import com.tlc.iam.server.internal.api.manager.OrganizationManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

/**
 * A listener that listens for events that is related to deactivate the organization
 *
 * @author AswiniN
 * @version 1.0
 */
@EventListener(name = IamEvents.ORG_STATE_MODIFIER)
public class OrgStateHandler implements EventConsumer<byte[]> {

    @Override
    public Boolean consume(final byte[] organizationBytes) {
        final JsonObject jsonObject = Json.object(organizationBytes);

        return jsonObject.containsKey(OrganizationFields.ID) && OrganizationManager.getInstance()
                .updateState(jsonObject.getLong(OrganizationFields.ID), jsonObject.optBoolean(OrganizationFields.IS_ACTIVE));
    }
}
