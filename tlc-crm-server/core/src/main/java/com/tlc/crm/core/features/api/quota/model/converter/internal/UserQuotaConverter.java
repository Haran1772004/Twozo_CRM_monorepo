package com.tlc.crm.core.features.api.quota.model.converter.internal;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.features.api.quota.fields.QuotaFields;
import com.tlc.crm.core.features.api.quota.model.QuotaHolder;
import com.tlc.crm.core.features.api.quota.model.UserQuota;
import com.tlc.iam.common.models.User;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Converter class for transforming between {@link UserQuota} and their JSON representations
 * for use in APIs and UI flows.
 *
 * @author Kishore J
 * @version 1.0
 */
public class UserQuotaConverter extends AbstractQuotaConverter<UserQuota, JsonObject> {

    private UserQuotaConverter() {
    }

    private static class Instance {
        private static final UserQuotaConverter INSTANCE = new UserQuotaConverter();
    }

    /**
     * <p>
     * Gets the {@link UserQuotaConverter}
     * </p>
     *
     * @return The {@link UserQuotaConverter}
     */
    public static UserQuotaConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterData the container encapsulating the input data to be converted (e.g., JsonObject or other source),
     *                      along with contextual information such as locale, user, or flags
     * @return The {@link UserQuota}
     */
    @Override
    public UserQuota convertToModel(final CrmConverterData<JsonObject> converterData) {
        final JsonObject dataJson = converterData.dataToBeConverted();
        final UserQuota userQuota;

        if (converterData.isPartialData()) {
            final QuotaHolder quotaHolder = quotaHolderManagerImpl.get(converterData.orgId(), dataJson.getLong(QuotaFields.ID));

            userQuota = quotaHolder.getUserQuota();
        } else {
            userQuota = new UserQuota();
        }

        if (dataJson.containsKey(QuotaFields.USERS)) {
            final JsonArray userArray = dataJson.getJsonArray(QuotaFields.USERS);
            final int size = userArray.size();
            final Collection<Long> userIds = new HashSet<>(size);

            for (int index = 0; index < size; index++) {
                userIds.add(userArray.getLong(index));
            }
            userQuota.setCrmUsers(userService.getUsers(converterData.orgId(), userIds));
        }
        loadQuotaFromJson(converterData, userQuota);

        return userQuota;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterData the container encapsulating the domain model instance along with contextual metadata
     * @return The {@link JsonObject}
     */
    @Override
    public JsonObject convertToJson(final CrmConverterData<UserQuota> converterData) {
        final UserQuota userQuota = converterData.dataToBeConverted();
        final JsonObject userQuotaJson = convertModelToJson(userQuota);
        final CrmUser crmUser = userQuota.getCrmUsers().stream().findFirst().orElseGet(() -> null);

        if (Objects.nonNull(crmUser)) {
            final JsonObject userJson = Json.object();
            final Long crmUserId = crmUser.id();
            final DisplayPicture thumbnail = crmUser.getThumbnail();

            userJson.put(QuotaFields.ID, crmUserId);
            userJson.put(QuotaFields.NAME, crmUser.getFullName());

            if (Objects.nonNull(thumbnail)) {
                userJson.put(QuotaFields.THUMBNAIL, crmUserConverter.fetchProfileJson(thumbnail, userQuota.orgId(), crmUserId));
            }
            userQuotaJson.put(QuotaFields.USER, userJson);
            userQuotaJson.put(QuotaFields.IS_ACTIVE_USER, Objects.equals(User.State.ACTIVE, crmUser.getState()));
        }
        return userQuotaJson;
    }
}
