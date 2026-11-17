package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.OrganizationFields;
import com.tlc.iam.common.models.converter.OrganizationConverter;
import com.tlc.iam.server.internal.api.manager.OrganizationManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

/**
 * A listener that listens for events that is related to fetch the organization
 *
 * @author Selvakumar
 * @version 1.0
 */
@EventListener(name = IamEvents.FETCH_ORGANIZATION)
public class OrganizationFetchHandler implements EventConsumer<byte[]> {

    @Override
    public byte[] consume(final byte[] jsonBytes) {
        final JsonObject orgJson = Json.object(jsonBytes);

        if (orgJson.containsKey(OrganizationFields.ID)) {
            final OrganizationManager organizationManager = OrganizationManager.getInstance();

            return OrganizationConverter.getInstance().convert(organizationManager.get(orgJson.getLong(OrganizationFields.ID))).getBytes();
        }

        return null;
    }
}
