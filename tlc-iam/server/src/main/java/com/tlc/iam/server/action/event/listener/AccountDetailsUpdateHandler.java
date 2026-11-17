package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.converter.UserConverter;
import com.tlc.iam.common.models.validator.EntityValidator;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.validator.type.Group;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Listens to the account details update event.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = IamEvents.ACCOUNT_DETAILS_UPDATE)
public class AccountDetailsUpdateHandler implements EventConsumer<byte[]> {

    /**
     * {@inheritDoc}
     *
     * @param data Represents the data to be consumed.
     */
    @Override
    public byte[] consume(final byte[] data) {
        final JsonObject response = Json.object();
        final UserManager userManager = UserManager.getInstance();
        final JsonObject userJson = Json.object(data).getJsonObject(UserFields.USER);
        final Long userId = userJson.getLong(UserFields.ID);
        final User user = userManager.get(userId);

        UserConverter.getInstance().mapJsonDataToUser(user, userJson.getLong(UserFields.ORG_ID), userJson);
        final Collection<String> violations = EntityValidator.validate(List.of(user), Group.Update.class);

        if (violations.isEmpty()) {
            final String mobile = user.getMobile();

            if (Objects.nonNull(mobile) && userManager.isMobileExists(user.orgId(), userId, mobile)) {
                response.put(IamFields.ERROR, "i18n.iam.accounts.mobile.exists");
            } else {
                userManager.updateAccountDetails(user);
            }
        } else {
            response.put(IamFields.ERRORS, violations);
        }

        return response.getBytes();
    }
}
