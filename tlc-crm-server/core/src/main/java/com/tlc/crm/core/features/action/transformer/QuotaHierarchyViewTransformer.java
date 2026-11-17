package com.tlc.crm.core.features.action.transformer;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.model.ListResponseStructureType;
import com.tlc.crm.common.list.transformer.model.TransformerRequest;
import com.tlc.crm.common.list.transformer.model.TransformerResponse;
import com.tlc.crm.core.features.action.quota.list.fields.QuotaListFields;
import com.tlc.crm.core.iam.role.permission.validator.QuotaPermissionValidator;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.l10n.temporal.model.DateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Transforms the quota hierarchy view entities into required forms.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@Transformer(name = "quotaHierarchyView", type = Transformer.Type.ENTITY)
public class QuotaHierarchyViewTransformer extends AbstractQuotaTransformer {

    private static final Collection<String> FIELDS_TO_INCLUDE = Set.of(QuotaListFields.USER_ID, QuotaListFields.ID,
            QuotaListFields.TARGET, QuotaListFields.TEAM_ID, QuotaListFields.METRIC, QuotaListFields.TEAM_NAME,
            QuotaListFields.TEAM_USER_QUOTA_ID, QuotaListFields.QUOTA, QuotaListFields.WON,
            QuotaListFields.QUOTA_WON_VALUE);

    /**
     * {@inheritDoc}
     *
     * @param transformerRequest Represents the {@link TransformerRequest}
     * @return The {@link TransformerResponse}
     */
    @Override
    public TransformerResponse transform(final TransformerRequest transformerRequest) {
        final ListResponseStructureType listResponseStructureType = transformerRequest.listResponseStructureType();
        final Collection<Map<String, Object>> data = groupRows(transformerRequest.records());
        final TransformerResponse transformerResponse = new TransformerResponse();
        final long orgId = transformerRequest.orgId();
        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final QuotaPermissionValidator quotaPermissionValidator = new QuotaPermissionValidator(orgId, crmUser.id(), crmUser.getRoleId());

        if (listResponseStructureType.isCommon()) {
            final Map<Long, List<Map<String, Object>>> rowsByUserId = new HashMap<>();

            for (final Map<String, Object> row : data) {

                if (row.get(QuotaListFields.USER) instanceof final Long userId) {
                    rowsByUserId.computeIfAbsent(userId, user -> new ArrayList<>()).add(new HashMap<>(row));
                }
            }
            final List<Map<String, Object>> results = new ArrayList<>(rowsByUserId.size());
            final Set<Long> userIds = new HashSet<>(rowsByUserId.size());

            for (final Map.Entry<Long, List<Map<String, Object>>> entry : rowsByUserId.entrySet()) {
                final Long userId = entry.getKey();
                final List<Map<String, Object>> records = entry.getValue();
                final Map<String, Object> dataMap;

                userIds.add(userId);
                final Map<String, Object> userWrapper = Map.of(QuotaListFields.USER, userId);

                if (records.size() == NumericalConstant.ONE) {
                    dataMap = records.get(NumericalConstant.ZERO);

                    loadActionMenuToRow(dataMap);
                } else {
                    dataMap = new HashMap<>();
                    final Map<String, Object> highestPeriodRecord = filterNonOverlappingQuotas(records).get(NumericalConstant.ZERO);

                    for (final Map.Entry<String, Object> field : highestPeriodRecord.entrySet()) {
                        final String fieldName = field.getKey();
                        final Object value = field.getValue();

                        if (isForecastCategoryField(fieldName) || (FIELDS_TO_INCLUDE.contains(fieldName) && Objects.nonNull(value))) {
                            dataMap.put(fieldName, value);
                        }
                    }
                    dataMap.put(QuotaListFields.USER_COUNT, records.size());
                }
                dataMap.put(QuotaListFields.USER, userId);
                dataMap.put(QuotaListFields.USER_ID, userWrapper);
                final ActionMenu actionMenu = new ActionMenu(false, false, false, false, false);

                buildActionMenuMap(actionMenu).forEach(dataMap::putIfAbsent);
                results.add(dataMap);
            }
            quotaPermissionValidator.loadQuotaPermissions(results);
            transformerResponse.setUserIds(userIds);
            transformerResponse.setResults(results);
        } else {
            final Collection<Long> userIds = new HashSet<>();

            for (final Map<String, Object> entityRow : data) {

                if (entityRow.containsKey(QuotaListFields.USER)
                        && entityRow.get(QuotaListFields.USER) instanceof final Long userId) {

                    userIds.add(userId);
                    entityRow.put(QuotaListFields.USER, userId);
                    entityRow.put(QuotaListFields.USER_ID, Map.of(QuotaListFields.USER, userId));
                }
                loadActionMenuToRow(entityRow);
            }
            quotaPermissionValidator.loadQuotaPermissions(data);
            transformerResponse.setUserIds(userIds);
            transformerResponse.setResults(data);
        }
        return transformerResponse;
    }

    /**
     * <p>
     * Loads action permissions on the given entity row based on quota and team context
     * </p>
     *
     * @param entityRow Represents the entity row
     */
    private void loadActionMenuToRow(final Map<String, Object> entityRow) {

        if (Objects.nonNull(entityRow.get(QuotaListFields.TEAM_USER_QUOTA_ID))) {
            final Long teamId = (Long) entityRow.get(QuotaListFields.TEAM_ID);
            final ActionMenu actionMenu;

            if (Objects.isNull(teamId)) {
                actionMenu = new ActionMenu(false, false, false, false, true);
            } else {
                actionMenu = new ActionMenu(true, true, true, true, false);
            }
            buildActionMenuMap(actionMenu).forEach(entityRow::putIfAbsent);
        } else {
            final ActionMenu actionMenu = new ActionMenu(true, true, true, true, true);

            buildActionMenuMap(actionMenu).forEach(entityRow::putIfAbsent);
        }
    }

    /**
     * <p>
     * Selects the highest-level non-overlapping quota records for display,
     * preventing double-counting across weekly / monthly / quarterly periods.
     * </p>
     *
     * <p>
     * Strategy — greedy longest-first selection:
     * <ol>
     *   <li>Sort all records by duration descending (quarterly > monthly > weekly)</li>
     *   <li>Greedily include a record only if its period does not overlap with
     *       any already-selected record</li>
     * </ol>
     * </p>
     *
     * <p>
     * Examples:
     * <ul>
     *   <li>Week1 + Week2 + Month → Month selected (longest), both weeks overlap → skipped.
     *       Display: Month value only</li>
     *   <li>Week1 + Week2 + Month + Q1 → Q1 selected first, Month overlaps Q1 → skipped,
     *       both weeks overlap Q1 → skipped. Display: Q1 value only</li>
     *   <li>Q1 + April(month) + Week-in-May → Q1 selected, April has no overlap with Q1 → selected,
     *       Week-in-May has no overlap → selected. Display: Q1 + April + Week-in-May</li>
     *   <li>Week1 + Week2, no month → Week1 selected, Week2 no overlap → selected.
     *       Display: Week1 + Week2</li>
     * </ul>
     * </p>
     *
     * @param records all quota records for a single user
     * @return the non-overlapping subset ordered from longest to shortest period
     */
    private List<Map<String, Object>> filterNonOverlappingQuotas(final List<Map<String, Object>> records) {
        final List<Map<String, Object>> sortedByDuration = records.stream()
                .sorted(Comparator.<Map<String, Object>>comparingLong(record ->
                                (getTimestamp(record, QuotaListFields.END_TIME)
                                        - getTimestamp(record, QuotaListFields.START_TIME))).reversed()
                        .thenComparing(Comparator.<Map<String, Object>>comparingLong(record -> getTimestamp(record, QuotaListFields.END_TIME)).reversed()))
                .toList();
        final List<Map<String, Object>> selectedData = new ArrayList<>();

        for (final Map<String, Object> row : sortedByDuration) {
            final long startTime = getTimestamp(row, QuotaListFields.START_TIME);
            final long endTime = getTimestamp(row, QuotaListFields.END_TIME);

            final boolean isOverlapsWithSelected = selectedData.stream().anyMatch(existing -> {
                final long existingStartTime = getTimestamp(existing, QuotaListFields.START_TIME);
                final long existingEndTime = getTimestamp(existing, QuotaListFields.END_TIME);

                return startTime < existingEndTime && endTime > existingStartTime;
            });

            if (!isOverlapsWithSelected) {
                selectedData.add(row);
            }
        }
        return selectedData;
    }

    /**
     * Checks whether the field represents a forecast category override or won value.
     *
     * @param fieldName the field key
     * @return true if the field is a forecast category value
     */
    private boolean isForecastCategoryField(final String fieldName) {
        return !fieldName.isEmpty() && (fieldName.startsWith(QuotaListFields.OVERRIDE_VALUE)
                || Character.isDigit(fieldName.charAt(0)));
    }

    /**
     * Safely extracts a timestamp (epoch millis) from a quota record field.
     *
     * @param record    the quota record map
     * @param fieldName the field key to read
     * @return the timestamp as a long, or 0 if absent/unparseable
     */
    private long getTimestamp(final Map<String, Object> record, final String fieldName) {
        final Object value = record.get(fieldName);

        if (value instanceof final DateTime dateTime) {
            return dateTime.getUtcTimeStamp();
        } else if (value instanceof final Number number) {
            return number.longValue();
        }
        return 0L;
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

        for (final Map<String, Object> sourceRow : quotaRows) {
            groupRows(sourceRow, (Long) sourceRow.get(QuotaListFields.TEAM_USER_QUOTA_ID),
                    List.of(QuotaListFields.USER_QUOTA), teamUserQuotaRows);
            groupRows(sourceRow, (Long) sourceRow.get(QuotaListFields.USER_QUOTA_ID),
                    List.of(QuotaListFields.TEAM_USER_QUOTA), userQuotaRows);
        }

        final Collection<Map<String, Object>> groupedQuotaRows = new ArrayList<>(teamUserQuotaRows.size() + userQuotaRows.size());

        groupedQuotaRows.addAll(teamUserQuotaRows.values());
        groupedQuotaRows.addAll(userQuotaRows.values());

        return groupedQuotaRows;
    }
}