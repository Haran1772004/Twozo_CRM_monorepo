package com.tlc.crm.core.action.transformer;

import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.features.action.quota.list.fields.QuotaListFields;
import com.tlc.crm.core.features.api.quota.model.QuotaTimePeriod;
import com.tlc.crm.core.features.api.quota.model.QuotaType;
import com.tlc.l10n.temporal.model.DateTime;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Manages quota user related transformer actions.
 *
 * @author Kishore J
 * @version 1.0
 */
@Transformer(name = "user", type = Transformer.Type.FIELD)
public class UserFieldTransFormer extends AbstractQuotaFieldTransformer {

    private static final String DELETED_TEAM_I18N_KEY = "i18n.quota.deleted.team";
    private static final String REMOVED_TEAM_USER_I18N_KEY = "i18n.quota.removed.team";

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organization id.
     * @param fieldName The name of the field to use for the transformation.
     * @param entity    The map of data to transform.
     * @return An Object representing the transformed.
     */
    @Override
    public Object transform(final long orgId, final String fieldName, final Map<String, Object> entity) {
        final Map<String, Object> transformedValue = new HashMap<>();

        if (entity.containsKey(QuotaListFields.START_TIME)) {
            transformedValue.put(QuotaListFields.START_TIME, ((DateTime) entity.get(QuotaListFields.START_TIME)).getTimeStamp());
        }

        if (entity.containsKey(QuotaListFields.TIME_PERIOD)) {
            transformedValue.put(QuotaListFields.TIME_PERIOD, resolveI18nValue(QuotaTimePeriod.getById((Short)
                    entity.get(QuotaListFields.TIME_PERIOD)).getDisplayName()));
        }

        if (entity.containsKey(QuotaListFields.END_TIME)) {
            transformedValue.put(QuotaListFields.END_TIME, ((DateTime) entity.get(QuotaListFields.END_TIME)).getTimeStamp());
        }

        if (Objects.nonNull(entity.get(QuotaListFields.TEAM_USER_QUOTA_ID))) {
            final Long teamId = (Long) entity.get(QuotaListFields.TEAM_ID);

            if (Objects.isNull(teamId)) {
                transformedValue.put(QuotaListFields.TEAM, Map.of(QuotaListFields.NAME, resolveI18nValue(DELETED_TEAM_I18N_KEY)));
            } else {
                final Object userTeamId = entity.get(QuotaListFields.USER_TEAM_ID);

                if (Objects.isNull(userTeamId)) {
                    transformedValue.put(QuotaListFields.TEAM, Map.of(QuotaListFields.NAME, resolveI18nValue(REMOVED_TEAM_USER_I18N_KEY)));
                } else {
                    transformedValue.put(QuotaListFields.TEAM, Map.of(QuotaListFields.ID, teamId, QuotaListFields.NAME, entity.get(QuotaListFields.TEAM_NAME)));
                }
            }
        }

        if (entity.containsKey(QuotaListFields.TYPE)) {
            transformedValue.put(QuotaListFields.TYPE, QuotaType.getById(NumberUtils.parseNumber(entity.get(QuotaListFields.TYPE)).shortValue()).name());
        }

        if (entity.containsKey(QuotaListFields.USER_COUNT)) {
            transformedValue.put(QuotaListFields.USER_COUNT, entity.get(QuotaListFields.USER_COUNT));
        }

        return transformedValue;
    }
}
