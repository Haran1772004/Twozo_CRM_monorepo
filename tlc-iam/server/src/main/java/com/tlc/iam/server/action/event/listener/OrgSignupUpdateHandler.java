package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.OrganizationFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.Organization;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.validator.EntityValidator;
import com.tlc.iam.server.internal.api.manager.OrganizationManager;
import com.tlc.iam.server.internal.api.manager.UserManager;

import com.tlc.validator.type.Group;

import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A listener that listens for events that is related to update the organization details while signup.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@EventListener(name = IamEvents.ORG_SIGNUP_UPDATE)
public class OrgSignupUpdateHandler implements EventConsumer<byte[]> {

    /**
     * {@inheritDoc}
     *
     * @param data Data to be consumed.
     */
    @Override
    public byte[] consume(final byte[] data) {
        final JsonObject jsonObject = Json.object(data);
        final long orgId = jsonObject.getLong(OrganizationFields.ID);
        final long userId = jsonObject.getLong(OrganizationFields.USER_ID);
        final OrganizationManager organizationManager = OrganizationManager.getInstance();
        final Organization organization = organizationManager.getUserOrganization(orgId, userId);

        if (Objects.nonNull(organization)) {
            final JsonObject response = Json.object();
            final User user = UserManager.getInstance().getUser(userId, orgId);

            organization.setName(jsonObject.getNullableString(OrganizationFields.COMPANY_NAME));
            organization.setOrgSize(jsonObject.containsKey(OrganizationFields.ORG_SIZE) ?
                    Organization.OrgSize.valueOf(jsonObject.getShort(OrganizationFields.ORG_SIZE)) : null);
            organization.setUsersSize(jsonObject.containsKey(OrganizationFields.USERS_SIZE) ?
                    Organization.UsersSize.valueOf(jsonObject.getShort(OrganizationFields.USERS_SIZE)) : null);
            organization.setIndustryType(jsonObject.containsKey(OrganizationFields.INDUSTRY_TYPE) ?
                    Organization.IndustryType.valueOf(jsonObject.getShort(OrganizationFields.INDUSTRY_TYPE)) : null);
            user.setJobTitle(jsonObject.getString(UserFields.JOB_TITLE));
            organization.setAdminUser(user);

            final Collection<String> violations = EntityValidator.validate(List.of(organization), Group.Update.class);

            if (violations.isEmpty()) {
                try {
                    organizationManager.updateSignupDetails(organization);
                    response.put(IamFields.MESSAGE, IamFields.SUCCESS);
                } catch (Exception exception) {
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
