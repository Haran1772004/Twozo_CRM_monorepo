package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.OrganizationFields;
import com.tlc.iam.common.models.Organization;
import com.tlc.iam.common.models.converter.OrganizationConverter;
import com.tlc.iam.common.models.validator.EntityValidator;
import com.tlc.iam.server.internal.api.manager.OrganizationManager;
import com.tlc.validator.type.Group;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A listener that listens for events that is related to fetch the organization
 *
 * @author Selvakumar
 * @version 1.0
 */
@EventListener(name = IamEvents.ORGANIZATION_UPDATE)
public class OrganizationUpdateHandler implements EventConsumer<byte[]> {

    @Override
    public byte[] consume(final byte[] orgBytes) {
        final JsonObject jsonObject = Json.object(orgBytes);
        final OrganizationManager organizationManager = OrganizationManager.getInstance();
        final Organization adminUserOrganization = organizationManager.getUserOrganization(jsonObject.getLong(OrganizationFields.ID),
                jsonObject.getLong(OrganizationFields.USER_ID));

        if (Objects.nonNull(adminUserOrganization)) {
            final JsonObject response = Json.object();
            final Organization organization = OrganizationConverter.getInstance().convert(jsonObject, adminUserOrganization);
            final Collection<String> violations = EntityValidator.validate(List.of(organization), Group.Update.class);

            if (violations.isEmpty()) {
                try {
                    organizationManager.update(organization);
                    response.put(IamFields.MESSAGE, IamFields.SUCCESS);
                } catch (Exception exception) {
                    response.put(IamFields.ERROR, exception.getMessage());
                    response.put(IamFields.MESSAGE, IamFields.FAILED);
                }
            } else {
                response.put(IamFields.ERRORS, violations);
            }

            return response.getBytes();
        }
        return null;
    }
}
