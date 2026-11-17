package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.jwt.meta.KeyGroup;
import com.tlc.commons.jwt.registry.KeyGroupRegistry;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.OrganizationFields;
import com.tlc.iam.common.fields.SessionFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.Organization;
import com.tlc.iam.common.models.UserSession;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.converter.OrganizationConverter;
import com.tlc.iam.common.models.converter.UserConverter;
import com.tlc.iam.common.models.validator.EntityValidator;
import com.tlc.iam.common.models.validator.type.Signup;
import com.tlc.iam.server.internal.api.manager.OrganizationManager;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.commons.crypto.HashGenerator;
import com.tlc.iam.server.internal.session.SessionGenerator;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A listener that listens for events that is related to signup activities
 *
 * @author Selvakumar
 * @version 1.0
 */

@EventListener(name = IamEvents.CREATE_ORGANIZATION)
public class SignupHandler implements EventConsumer<byte[]> {

    @Override
    public byte[] consume(final byte[] orgBytes) {
        final JsonObject data = Json.object(orgBytes);
        final User adminUser = UserConverter.getInstance().convert(null, data.getJsonObject(UserFields.USER));
        final Organization organization = data.containsKey(OrganizationFields.ORGANIZATION)
                ? OrganizationConverter.getInstance().convert(data.getJsonObject(OrganizationFields.ORGANIZATION))
                : new Organization();
        final JsonObject response = Json.object();

        adminUser.setType(User.Type.ORG_ADMIN);
        adminUser.setUserState(User.State.ACTIVE);
        organization.setAdminUser(adminUser);
        organization.setOrgState(Organization.State.ACTIVE);

        final Collection<String> violations = EntityValidator.validate(List.of(organization), Signup.class);

        if (violations.isEmpty()) {
            final UserManager userManager = UserManager.getInstance();
            final String email = adminUser.getEmail();

            if ((Objects.nonNull(email) && userManager.isEmailAddressExists(email))) {
                response.put(IamFields.ERRORS, "i18n.iam.accounts.login.provider.account.exists");
            } else {
                final String keyGroupName = data.containsKey(IamFields.APPLICATION) ? data.getString(IamFields.APPLICATION) : IamFields.DEFAULT;
                final KeyGroup keyGroup = KeyGroupRegistry.getInstance().get(keyGroupName.toUpperCase());

                adminUser.setPasswordHash(HashGenerator.getInstance().generateHash(adminUser.getPassword()));
                OrganizationManager.getInstance().save(organization);
                final UserSession userSession = SessionGenerator.getInstance().generate(keyGroup, adminUser, data.getJsonObject(SessionFields.USER_AGENT));

                response.put(IamFields.ACCESS_TOKEN, userSession.getToken());
                response.put(OrganizationFields.ID, organization.id());
                response.put(OrganizationFields.USER_ID, organization.getAdminUser().id());
                response.put(UserFields.FULL_NAME, adminUser.getFullName());
            }
        } else {
            response.put(IamFields.ERRORS, violations);
        }

        return response.getBytes();
    }


}
