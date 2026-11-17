package com.tlc.crm.common.iam.internal.manager.impl;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;
import com.tlc.crm.common.service.UserPreferenceHolderService;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.common.iam.api.manager.ActionLimitManager;
import com.tlc.crm.security.permission.model.ActionLimitDetails;
import com.tlc.crm.security.permission.model.LimitActionType;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.security.sql.resource.OIAMPACTIONLIMITDETAILS;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.service.TemporalService;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the action limit count related DB actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class ActionLimitManagerImpl implements ActionLimitManager {

    private final UserPreferenceHolderService userPreferenceService;

    private ActionLimitManagerImpl() {
        userPreferenceService = UserPreferenceHolderService.get();
    }

    private static final class InstanceHolder {
        private static final ActionLimitManagerImpl ACTION_LIMIT_MANAGER = new ActionLimitManagerImpl();
    }

    public static ActionLimitManagerImpl getInstance() {
        return InstanceHolder.ACTION_LIMIT_MANAGER;
    }

    private Table getTable() {
        return Table.get(OIAMPACTIONLIMITDETAILS.TABLE);
    }

    /**
     * <p>
     * Provides the {@link OrgDataStore} to handle the DB related actions
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * <p>
     * Provides the {@link AdminDataStore} to handle the DB related actions
     * </p>
     *
     * @return The {@link AdminDataStore}
     */
    private AdminDataStore adminDataStore() {
        return DataStoreProvider.getAdminDataStore();
    }

    /**
     * {@inheritDoc}
     *
     * @param actionLimitDetails Holds the details of the action limit count.
     * @return Remaining action count of the given {@link LimitActionType}.
     */
    @Override
    public Optional<Long> getRemainingActionLimit(final ActionLimitDetails actionLimitDetails) {
        return getActionCount(actionLimitDetails, getTable().getColumn(OIAMPACTIONLIMITDETAILS.REMAINING_COUNT));
    }

    /**
     * {@inheritDoc}
     *
     * @param actionLimitDetails Holds the details of the action limit count.
     * @return Currently used count of the given {@link LimitActionType}.
     */
    @Override
    public long getCurrentlyUsedLimit(final ActionLimitDetails actionLimitDetails) {
        return getActionCount(actionLimitDetails, getTable().getColumn(OIAMPACTIONLIMITDETAILS.CURRENTLY_USED_COUNT)).orElseGet(() -> 0L);
    }

    /**
     * {@inheritDoc}
     *
     * @param actionLimitDetails Holds the details of the action limit count.
     */
    @Override
    public void store(final ActionLimitDetails actionLimitDetails) {
        final Table table = getTable();
        final Long orgId = actionLimitDetails.orgId();
        final Long userId = actionLimitDetails.getUserId();
        final LimitActionType limitActionType = actionLimitDetails.getLimitActionType();
        final short actionTypeId = limitActionType.getId();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final DataContainer dataContainer = orgDataStore.get(table, buildWhereClause(userId, actionTypeId));
        final Row actionCountRow = dataContainer.isEmpty() ? new Row(table) : dataContainer.getRow(table);
        final DataContainer targetContainer = DataContainer.create();

        if (actionCountRow.isNewRow()) {
            final TimezoneInfo userTimezone = getUserTimezone();
            final Long expirationTime = TemporalServiceProvider.get().getEndTimeOfDay(userTimezone);

            actionCountRow.setOrgId(orgId);
            actionCountRow.set(OIAMPACTIONLIMITDETAILS.USER_ID, userId);
            actionCountRow.set(OIAMPACTIONLIMITDETAILS.ACTION_TYPE, actionTypeId);
            actionCountRow.set(OIAMPACTIONLIMITDETAILS.EXPIRATION_TIME, new DateTime(expirationTime, userTimezone));
            actionCountRow.set(OIAMPACTIONLIMITDETAILS.CURRENTLY_USED_COUNT, actionLimitDetails.getCurrentlyUsedCount());
        }
        actionCountRow.set(OIAMPACTIONLIMITDETAILS.REMAINING_COUNT, actionLimitDetails.getRemainingActionCount());
        targetContainer.updateRow(actionCountRow);

        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId              Represents the organisation id.
     * @param actionLimitDetails Holds the details of the action limit count.
     */
    @Override
    public void updateRemainingActionLimit(final long orgId, final Collection<ActionLimitDetails> actionLimitDetails) {
        final Table table = getTable();
        final List<UpdateQuery> updateQueries = new ArrayList<>();

        for (final ActionLimitDetails actionLimitDetail : actionLimitDetails) {
            final UpdateQuery updateQuery = UpdateQuery.get(table);

            updateQuery.addUpdateColumn(table.getColumn(OIAMPACTIONLIMITDETAILS.REMAINING_COUNT), actionLimitDetail.getRemainingActionCount());
            updateQuery.setWhereClause(buildWhereClause(actionLimitDetail.getUserId(), actionLimitDetail.getLimitActionType().getId()));
            updateQueries.add(updateQuery);
        }
        orgDataStore(orgId).update(updateQueries);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId              Represents the organisation id.
     * @param actionLimitDetails Holds the details of the action limit count.
     */
    @Override
    public void updateCurrentlyUsedLimit(final long orgId, final Collection<ActionLimitDetails> actionLimitDetails) {
        final Table table = getTable();
        final List<UpdateQuery> updateQueries = new ArrayList<>();

        for (final ActionLimitDetails actionLimitDetail : actionLimitDetails) {
            final UpdateQuery updateQuery = UpdateQuery.get(table);

            updateQuery.addUpdateColumn(table.getColumn(OIAMPACTIONLIMITDETAILS.CURRENTLY_USED_COUNT), actionLimitDetail.getCurrentlyUsedCount());
            updateQuery.setWhereClause(buildWhereClause(actionLimitDetail.getUserId(), actionLimitDetail.getLimitActionType().getId()));
            updateQueries.add(updateQuery);
        }
        orgDataStore(orgId).update(updateQueries);
    }

    /**
     * {@inheritDoc}
     *
     * @param actionLimitDetails Holds the details of the action limit count.
     */
    @Override
    public void remove(final ActionLimitDetails actionLimitDetails) {
        final Table table = getTable();
        final DeleteQuery deleteQuery = DeleteQuery.get(table);

        deleteQuery.setWhereClause(buildWhereClause(actionLimitDetails.getUserId(), actionLimitDetails.getLimitActionType().getId()));
        orgDataStore(actionLimitDetails.orgId()).delete(deleteQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @return Expired action limits count.
     */
    @Override
    public long getExpiredActionLimitsCount() {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final TemporalService temporalService = TemporalServiceProvider.get(getUserTimezone());

        countQuery.setWhereClause(new WhereClause(Criteria.le(table.getColumn(OIAMPACTIONLIMITDETAILS.EXPIRATION_TIME), temporalService.getUtcDateTime())));
        return adminDataStore().get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param maxNumberOfRecords Represents the count which refers maximum number of records to be fetched.
     * @return Expired action limit count IDs.
     */
    @Override
    public Collection<Long> getExpiredActionLimitIds(final int maxNumberOfRecords) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final TemporalService temporalService = TemporalServiceProvider.get(getUserTimezone());

        selectQuery.addSelectClause(table.getPKColumn());
        selectQuery.setLimitClause(new LimitClause(maxNumberOfRecords));
        selectQuery.setWhereClause(new WhereClause(Criteria.le(table.getColumn(OIAMPACTIONLIMITDETAILS.EXPIRATION_TIME), temporalService.getUtcDateTime())));

        return adminDataStore().get(selectQuery).getRows(table).map(Row::getPKValue).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param actionLimitIds Action limit IDs.
     */
    @Override
    public void delete(final Collection<Long> actionLimitIds) {
        adminDataStore().delete(getTable(), actionLimitIds);
    }

    /**
     * <p>
     * Fetches the Action count of the given {@link LimitActionType}.
     * </p>
     *
     * @param actionLimitDetails Holds the details of the action limit count.
     * @param countColumn        Represents the column of the action count.
     * @return Action count of the given {@link LimitActionType}.
     */
    private Optional<Long> getActionCount(final ActionLimitDetails actionLimitDetails, final Column countColumn) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(countColumn);
        selectQuery.setWhereClause(buildWhereClause(actionLimitDetails.getUserId(), actionLimitDetails.getLimitActionType().getId()));
        final Optional<Row> actionCountRow = Optional.ofNullable(orgDataStore(actionLimitDetails.orgId()).get(selectQuery).getRow(table));

        return actionCountRow.map(row -> row.get(countColumn));
    }

    /**
     * <p>
     * Builds the where clause to fetch the remaining limit count of the given {@link LimitActionType}.
     * </p>
     *
     * @param userId        Represents the user id.
     * @param actionTypeId Represents the id of the {@link LimitActionType}.
     * @return {@link WhereClause}.
     */
    private WhereClause buildWhereClause(final long userId, final short actionTypeId) {
        final Table table = getTable();

        return new WhereClause(Criteria.eq(table.getColumn(OIAMPACTIONLIMITDETAILS.USER_ID), userId))
                .and(Criteria.eq(table.getColumn(OIAMPACTIONLIMITDETAILS.ACTION_TYPE), actionTypeId));
    }

    /**
     * <p>
     * Provides the preferred {@link TimezoneInfo} of the current user.
     * </p>
     *
     * @return {@link TimezoneInfo}
     */
    private TimezoneInfo getUserTimezone() {
        final CrmUser user = LoggedInUserInfo.getUser();
        final UserRegionalPreference userPreference = userPreferenceService.get(user.orgId(), user.id())
                .get(UserPreferenceType.USER_REGIONAL_PREFERENCE);

        return userPreference.getTimezoneInfo();
    }
}