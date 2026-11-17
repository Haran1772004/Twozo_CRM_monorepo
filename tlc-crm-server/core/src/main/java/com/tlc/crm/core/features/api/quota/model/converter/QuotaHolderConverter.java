package com.tlc.crm.core.features.api.quota.model.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.CrmModelConverter;
import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.core.features.api.quota.fields.QuotaFields;
import com.tlc.crm.core.features.api.quota.model.QuotaHolder;
import com.tlc.crm.core.features.api.quota.model.QuotaType;
import com.tlc.crm.core.features.api.quota.model.TeamQuota;
import com.tlc.crm.core.features.api.quota.model.UserQuota;
import com.tlc.crm.core.features.api.quota.model.converter.internal.TeamQuotaConverter;
import com.tlc.crm.core.features.api.quota.model.converter.internal.UserQuotaConverter;

/**
 * Converter class for transforming between {@link QuotaHolder} and their JSON representations
 * for use in APIs and UI flows.
 *
 * @author Kishore J
 * @version 1.0
 */
public class QuotaHolderConverter extends CrmModelConverter<QuotaHolder, JsonObject> {

    private final TeamQuotaConverter teamQuotaConverter;
    private final UserQuotaConverter userQuotaConverter;

    private QuotaHolderConverter() {
        teamQuotaConverter = TeamQuotaConverter.getInstance();
        userQuotaConverter = UserQuotaConverter.getInstance();
    }

    private static class Instance {
        private static final QuotaHolderConverter INSTANCE = new QuotaHolderConverter();
    }

    /**
     * <p>
     * Gets the {@link QuotaHolderConverter}
     * </p>
     *
     * @return The {@link QuotaHolderConverter}
     */
    public static QuotaHolderConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterData the container encapsulating the input data to be converted (e.g., JsonObject or other source),
     *                      along with contextual information such as locale, user, or flags
     * @return The QuotaHolder
     */
    public QuotaHolder convertToModel(final CrmConverterData<JsonObject> converterData) {
        final JsonObject quotaJson = converterData.dataToBeConverted();
        final QuotaType quotaType = QuotaType.getById(quotaJson.getShort(QuotaFields.TYPE));
        final QuotaHolder quotaHolder = new QuotaHolder();
        final CrmConverterData<JsonObject> quotaConverterData = new CrmConverterData<>(converterData.getUser(),
                converterData.locale(), true, false, quotaJson);

        if (quotaType.isTeamQuota()) {
            final TeamQuota teamQuota = teamQuotaConverter.convertToModel(quotaConverterData);

            quotaHolder.setId(teamQuota.id());
            quotaHolder.setTeamQuota(teamQuota);
        } else {
            final UserQuota userQuota = userQuotaConverter.convertToModel(quotaConverterData);

            quotaHolder.setId(userQuota.id());
            quotaHolder.setUserQuota(userQuota);
        }
        quotaHolder.setOrgId(converterData.orgId());
        return quotaHolder;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterData the container encapsulating the domain model instance along with contextual metadata
     * @return The {@link JsonObject}
     */
    @Override
    public JsonObject convertToJson(final CrmConverterData<QuotaHolder> converterData) {
        final QuotaHolder quotaHolder = converterData.dataToBeConverted();
        final JsonObject jsonObject;

        if (quotaHolder.hasTeamQuota()) {
            final CrmConverterData<TeamQuota> teamQuotaConverterData = new CrmConverterData<>(converterData.getUser(),
                    converterData.locale(), true, false, quotaHolder.getTeamQuota());

            jsonObject = teamQuotaConverter.convertToJson(teamQuotaConverterData);
        } else {
            final CrmConverterData<UserQuota> userQuotaConverterData = new CrmConverterData<>(converterData.getUser(),
                    converterData.locale(), true, false, quotaHolder.getUserQuota());

            jsonObject = userQuotaConverter.convertToJson(userQuotaConverterData);
        }
        return jsonObject;
    }
}
