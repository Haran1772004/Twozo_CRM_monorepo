package com.tlc.crm.core.features.action.transformer;

import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.list.transformer.ModelTransformer;
import com.tlc.crm.core.features.action.quota.list.fields.QuotaListFields;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Transforms the quota view entities into required forms.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public abstract class AbstractQuotaTransformer implements ModelTransformer {

    /**
     * <p>
     * Groups a source quota row into an aggregated quota map.
     * </p>
     *
     * @param sourceRow     the raw quota row to be merged
     * @param quotaId       the identifier used for grouping the quota rows
     * @param quotaSuffixes Which contains collection of quota suffix used to filter out excluded fields
     * @param quotaMap      the target map holding aggregated quota rows
     */
    protected void groupRows(final Map<String, Object> sourceRow, final Long quotaId,
                             final Collection<String> quotaSuffixes, final Map<Long, Map<String, Object>> quotaMap) {
        if (Objects.isNull(quotaId)) {
            return;
        }
        final Map<String, Object> targetRow = quotaMap.computeIfAbsent(quotaId, id -> new HashMap<>());
        final String underScore = StringUtil.Delimiter.UNDER_SCORE.getDelimiter();

        for (final Map.Entry<String, Object> entry : sourceRow.entrySet()) {
            final String key = entry.getKey();
            final Object fieldValue = entry.getValue();
            final int underscoreIndex = key.indexOf(underScore);
            final boolean hasDelimiter = underscoreIndex != NumericalConstant.NEGATIVE_ONE;
            final String fieldName = hasDelimiter ? key.substring(NumericalConstant.ZERO, underscoreIndex) : key;
            boolean isExcluded = false;

            if (hasDelimiter) {
                final String quotaSuffix = key.substring(underscoreIndex + NumericalConstant.ONE);
                isExcluded = quotaSuffixes.contains(quotaSuffix);
            }
            final boolean startsWithDigit = !fieldName.isEmpty() && Character.isDigit(fieldName.charAt(0));

            if (isExcluded || (!startsWithDigit && Objects.isNull(fieldValue))) {
                continue;
            }

            if (!targetRow.containsKey(fieldName) || Objects.nonNull(fieldValue)) {
                targetRow.put(fieldName, fieldValue);
            }
        }
    }

    /**
     * Represents the set of permitted actions for a quota entity.
     *
     * @param canView                   indicates whether the entity can be viewed
     * @param canEdit                   indicates whether the entity can be edited
     * @param canClone                  indicates whether the entity can be cloned
     * @param canRecalculateAchievement indicates whether achievement recalculation is allowed
     * @param canDelete                 indicates whether the entity can be deleted
     * @author Kishore J
     * @version 1.0
     */
    protected record ActionMenu(boolean canView, boolean canEdit, boolean canClone, boolean canRecalculateAchievement,
                                boolean canDelete) {
    }

    /**
     * <p>
     * Builds the action menu map based on the permissions.
     * </p>
     *
     * @param actionMenu Represents the {@link ActionMenu}
     * @return The action menu map
     */
    protected Map<String, Boolean> buildActionMenuMap(final ActionMenu actionMenu) {
        final Map<String, Boolean> actionMenuMap = new HashMap<>();

        actionMenuMap.put(QuotaListFields.CAN_VIEW, actionMenu.canView());
        actionMenuMap.put(QuotaListFields.CAN_EDIT, actionMenu.canEdit());
        actionMenuMap.put(QuotaListFields.CAN_CLONE, actionMenu.canClone());
        actionMenuMap.put(QuotaListFields.CAN_RECALCULATE_ACHIEVEMENT, actionMenu.canRecalculateAchievement());
        actionMenuMap.put(QuotaListFields.CAN_DELETE, actionMenu.canDelete());

        return actionMenuMap;
    }
}
