package com.tlc.crm.core.action.transformer;

import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.core.features.action.quota.list.fields.QuotaListFields;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Manages user and team field related transformer actions.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@Transformer(name = "userAndTeam", type = Transformer.Type.FIELD)
public class UserAndTeamTransformer extends AbstractQuotaFieldTransformer {

    private static final String USER = "USER";
    private static final String TEAM_USER = "TEAM_USER";
    private static final String TEAM = "TEAM";
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

        if (Objects.isNull(entity.get(QuotaListFields.TEAM_QUOTA_BASE_QUOTA_ID))) {
            final Object teamUserQuotaId = entity.get(QuotaListFields.TEAM_USER_QUOTA_ID);

            if (Objects.isNull(teamUserQuotaId)) {
                transformedValue.put(QuotaListFields.ICON, USER);

                return transformedValue;
            }
            final Object teamId = entity.get(QuotaListFields.TEAM_ID);

            if (Objects.isNull(teamId)) {
                transformedValue.put(QuotaListFields.TEAM, Map.of(QuotaListFields.NAME, resolveI18nValue(DELETED_TEAM_I18N_KEY)));
            } else {
                final Object userTeamId = entity.get(QuotaListFields.USER_TEAM_ID);

                if (Objects.isNull(userTeamId)) {
                    transformedValue.put(QuotaListFields.TEAM, Map.of(QuotaListFields.NAME, resolveI18nValue(REMOVED_TEAM_USER_I18N_KEY)));
                } else {
                    addTeamInfo(transformedValue, teamId, entity.get(QuotaListFields.TEAM_NAME));
                }
            }
            transformedValue.put(QuotaListFields.ICON, TEAM_USER);

            return transformedValue;
        }
        final Object teamName = entity.get(QuotaListFields.TEAM_QUOTA_TEAM_NAME);

        if (Objects.isNull(teamName)) {
            transformedValue.put(QuotaListFields.TEAM, Map.of(QuotaListFields.NAME, resolveI18nValue(DELETED_TEAM_I18N_KEY)));
        } else {
            addTeamInfo(transformedValue, entity.get(QuotaListFields.TEAM_QUOTA_TEAM_ID), teamName);
        }
        transformedValue.put(QuotaListFields.ICON, TEAM);

        return transformedValue;
    }

    /**
     * <p>
     * Adds team information into the given target map
     * </p>
     *
     * @param target   The target map to which team information will be added
     * @param teamId   The team identifier, may be if the team is deleted
     * @param teamName The team name associated with the team
     */
    private void addTeamInfo(final Map<String, Object> target, final Object teamId, final Object teamName) {

        if (Objects.isNull(teamId)) {
            target.put(QuotaListFields.TEAM, Map.of(QuotaListFields.NAME, resolveI18nValue(DELETED_TEAM_I18N_KEY)));
            return;
        }
        target.put(QuotaListFields.TEAM, Map.of(QuotaListFields.ID, teamId, QuotaListFields.NAME, teamName));
    }
}