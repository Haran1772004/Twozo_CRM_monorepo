package com.tlc.crm.core.features.action.transformer;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.model.TransformerRequest;
import com.tlc.crm.common.list.transformer.model.TransformerResponse;
import com.tlc.crm.core.features.action.quota.list.fields.QuotaListFields;
import com.tlc.crm.core.iam.role.permission.validator.QuotaPermissionValidator;
import com.tlc.crm.security.iam.LoggedInUserInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Comparator;

/**
 * <p>
 * Transforms the team view quota entities into required forms.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@Transformer(name = "quotaTeamView", type = Transformer.Type.ENTITY)
public class QuotaTeamViewTransformer extends AbstractQuotaTransformer {

    /**
     * {@inheritDoc}
     *
     * @param transformerRequest Represents the {@link TransformerRequest}
     * @return The {@link TransformerResponse}
     */
    @Override
    public TransformerResponse transform(final TransformerRequest transformerRequest) {
        final TransformerResponse transformerResponse = new TransformerResponse();
        final Collection<Map<String, Object>> data = groupRows(transformerRequest.records());
        final Set<Long> userIds = new HashSet<>();
        final long orgId = transformerRequest.orgId();
        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final QuotaPermissionValidator quotaPermissionValidator = new QuotaPermissionValidator(orgId, crmUser.id(), crmUser.getRoleId());

        data.forEach(entityRow -> {
            final Object teamQuotaBaseQuotaId = entityRow.get(QuotaListFields.TEAM_QUOTA_BASE_QUOTA_ID);

            if (entityRow.get(QuotaListFields.USER_AND_TEAM) instanceof final Long userId && Objects.isNull(teamQuotaBaseQuotaId)) {
                userIds.add(userId);
                entityRow.put(QuotaListFields.USER, userId);

                final Map<String, Object> userMap = new HashMap<>();
                final Map<String, Object> users = new HashMap<>();

                userMap.put(QuotaListFields.USER, userId);
                users.put(QuotaListFields.USER_AND_TEAM, userMap);
                entityRow.put(QuotaListFields.USER_ID, users);
            }
            final ActionMenu actionMenu;
            final boolean hasTeamUserQuotaId = Objects.nonNull(entityRow.get(QuotaListFields.TEAM_USER_QUOTA_ID));

            if (hasTeamUserQuotaId) {
                final boolean hasTeamId = Objects.nonNull(entityRow.get(QuotaListFields.TEAM_ID));

                actionMenu = hasTeamId
                        ? new ActionMenu(true, true, true, true,
                        Objects.nonNull(entityRow.get(QuotaListFields.TEAM_QUOTA_BASE_QUOTA_ID)))
                        : new ActionMenu(false, false, false, false, true);
            } else {
                final boolean isDeleteTable = Objects.nonNull(entityRow.get(QuotaListFields.USER_QUOTA_ID))
                        || Objects.nonNull(entityRow.get(QuotaListFields.TEAM_QUOTA_BASE_QUOTA_ID));

                actionMenu = new ActionMenu(true, true, true, true, isDeleteTable);
            }
            buildActionMenuMap(actionMenu).forEach(entityRow::putIfAbsent);
        });
        quotaPermissionValidator.loadQuotaPermissions(data);

        transformerResponse.setResults(data);
        transformerResponse.setUserIds(userIds);
        return transformerResponse;
    }

    /**
     * <p>
     * Groups quota rows by team-user quota ID and user quota ID.
     * </p>
     *
     * @param quotaRows the collection of raw quota rows
     * @return a collection of grouped quota rows
     */
    private Collection<Map<String, Object>> groupRows(final Collection<Map<String, Object>> quotaRows) {
        final Map<Long, Map<String, Object>> teamUserQuotaRows = new LinkedHashMap<>();
        final Map<Long, Map<String, Object>> userQuotaRows = new LinkedHashMap<>();
        final Map<Long, Map<String, Object>> teamQuotaRows = new LinkedHashMap<>();

        for (final Map<String, Object> sourceRow : quotaRows) {
            groupRows(sourceRow, (Long) sourceRow.get(QuotaListFields.TEAM_USER_QUOTA_ID),
                    List.of(QuotaListFields.USER_QUOTA, QuotaListFields.TEAM_QUOTA), teamUserQuotaRows);
            groupRows(sourceRow, (Long) sourceRow.get(QuotaListFields.USER_QUOTA_ID),
                    List.of(QuotaListFields.TEAM_USER_QUOTA, QuotaListFields.TEAM_QUOTA), userQuotaRows);
            groupRows(sourceRow, (Long) sourceRow.get(QuotaListFields.TEAM_QUOTA_ID),
                    List.of(QuotaListFields.USER_QUOTA, QuotaListFields.TEAM_USER_QUOTA), teamQuotaRows);
        }
        final List<Map<String, Object>> groupedQuotaRows = new ArrayList<>(teamUserQuotaRows.size() + userQuotaRows.size() + teamQuotaRows.size());

        groupedQuotaRows.addAll(teamUserQuotaRows.values());
        groupedQuotaRows.addAll(userQuotaRows.values());
        groupedQuotaRows.addAll(teamQuotaRows.values());
        groupedQuotaRows.sort(Comparator.comparingDouble(this::computeRowWonPercentage).reversed());

        return groupedQuotaRows;
    }


    /**
     * <p>
     * Computes the won percentage ((won / target) * 100) for a grouped quota row,
     * used to sort rows by achievement.
     * </p>
     *
     * @param row Represents a grouped quota row containing won and target values
     * @return The won percentage, or 0 if values are missing, non-numeric, or target is not positive
     */
    private double computeRowWonPercentage(final Map<String, Object> row) {
        final Object won = row.get(QuotaListFields.QUOTA_WON_VALUE);
        final Object target = row.get(QuotaListFields.QUOTA);

        if (won instanceof Number wonNum && target instanceof Number targetNum && targetNum.doubleValue() > 0) {
            return (wonNum.doubleValue() * NumericalConstant.ONE_HUNDRED) / targetNum.doubleValue();
        }

        return NumericalConstant.ZERO;
    }
}
