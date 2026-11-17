package com.tlc.crm.common.iam.api.preferences.manager.impl;

import com.tlc.cluster.DistributedLock;
import com.tlc.cluster.service.ClusterService;
import com.tlc.cluster.service.provider.ClusterServiceProvider;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.preferences.manager.UserPreferenceManager;
import com.tlc.crm.common.iam.api.preferences.model.Preference;
import com.tlc.crm.common.iam.api.preferences.model.UserPreference;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.crm.common.sql.resource.OMCRMPREFERENCE;
import com.tlc.crm.common.sql.resource.OMCRMUSERPREFERENCE;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * <p>
 * Implements the {@link UserPreferenceHolder} related DB actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class UserPreferenceManagerImpl extends AbstractPreferenceManager<UserPreferenceHolder> implements UserPreferenceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserPreferenceManagerImpl.class);
    private static final String TLC_CRM_USER_PREFERENCE_PATH = "tlc/crm/user/preference";

    private final ClusterService clusterService;

    private UserPreferenceManagerImpl() {
        clusterService = ClusterServiceProvider.get();
    }

    private static final class Instance {
        private static final UserPreferenceManagerImpl PREFERENCE_MANGER = new UserPreferenceManagerImpl();
    }

    public static UserPreferenceManagerImpl getInstance() {
        return Instance.PREFERENCE_MANGER;
    }

    /**
     * <p>
     * Gets the preference table.
     * </p>
     *
     * @return The preference table.
     */
    private Table getUserPreferenceTable() {
        return Table.get(OMCRMUSERPREFERENCE.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id.
     * @param userId Represents the user id.
     * @return The user {@link Preference} details.
     */
    @Override
    public UserPreferenceHolder get(final long orgId, final long userId) {
        final Table preferenceTable = getTable();
        final DataContainer dataContainer = getUserPreferenceDataContainer(orgId, userId);
        final Optional<Row> optionalPreferenceRow = Optional.ofNullable(dataContainer.getRow(preferenceTable));

        return optionalPreferenceRow.map(preferenceRow -> convertRowToModel(userId, preferenceRow)).orElseGet(() -> null);
    }

    /**
     * <p>
     * Fetches the user's id
     * </p>
     *
     * @param orgId        Represents an organization id.
     * @param preferenceId Represents the {@link UserPreference} id.
     * @return The {@link UserPreference} details.
     */
    @Override
    public Long getUserId(final long orgId, final long preferenceId) {
        final Table preferenceTable = getTable();
        final Table userPreferenceTable = getUserPreferenceTable();
        final SortedSet<Table> tables = new TreeSet<>();

        tables.add(preferenceTable);
        tables.add(userPreferenceTable);
        final DataContainer dataContainer = getOrgDataStore(orgId).get(tables, new WhereClause(Criteria.eq(preferenceTable.getPKColumn(), preferenceId)), List.of(userPreferenceTable.getColumn(OMCRMUSERPREFERENCE.USER_ID)));
        final Row userPreferenceRow = dataContainer.getRow(userPreferenceTable);

        return Objects.isNull(userPreferenceRow) ? null : userPreferenceRow.get(OMCRMUSERPREFERENCE.USER_ID);
    }

    /**
     * {@inheritDoc}
     *
     * @param userPreference Represents the {@link UserPreference} details.
     */
    @Override
    public void updatePreference(final UserPreference userPreference) {
        final UserPreferenceHolder preferenceHolder = get(userPreference.orgId(), userPreference.userId());

        preferenceHolder.add(userPreference);
        update(preferenceHolder);
    }

    /**
     * {@inheritDoc}
     *
     * @param isCreateEvent   A flag indicating whether the operation is triggered by a create event.
     * @param preferenceHolder     Represents an {@link UserPreference} details.
     * @param sourceContainer Holds the existing details of the preferences.
     * @param targetContainer Holds the details of the preferences.
     */
    @Override
    public void loadPreferencesIntoContainer(final boolean isCreateEvent, final UserPreferenceHolder preferenceHolder,
                                             final DataContainer sourceContainer, final DataContainer targetContainer) {
        super.loadPreferencesIntoContainer(isCreateEvent, preferenceHolder, sourceContainer, targetContainer);

        if (isCreateEvent) {
            final Row userPreferenceRow = new Row(getUserPreferenceTable());

            userPreferenceRow.setOrgId(preferenceHolder.orgId());
            userPreferenceRow.set(OMCRMUSERPREFERENCE.USER_ID, preferenceHolder.getUserId());
            userPreferenceRow.set(OMCRMUSERPREFERENCE.PREFERENCE_ID, preferenceHolder.id());

            targetContainer.addNewRow(userPreferenceRow);
        } else if (preferenceHolder.isUpdated()) {
            final Table userPreferenceTable = getUserPreferenceTable();
            final Row userPreferenceRow = sourceContainer.getRow(getUserPreferenceTable(), new WhereClause(Criteria.eq(userPreferenceTable
                    .getColumn(OMCRMUSERPREFERENCE.USER_ID), preferenceHolder.getUserId()))
                    .and(Criteria.eq(userPreferenceTable.getColumn(OMCRMUSERPREFERENCE.IS_UPDATED), false)));

            if (Objects.nonNull(userPreferenceRow)) {
                userPreferenceRow.set(OMCRMUSERPREFERENCE.IS_UPDATED, true);

                targetContainer.updateRow(userPreferenceRow);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param isCreateEvent        A flag indicating whether the operation is triggered by a create event.
     * @param userPreferenceHolder Represents an {@link Preference} details.
     * @param targetContainer      Holds the details of the preferences.
     */
    @Override
    public void loadPreferencesIntoContainer(final boolean isCreateEvent, final UserPreferenceHolder userPreferenceHolder,
                                             final DataContainer targetContainer) {
        final JsonObject preferenceJson = userPreferenceHolder.toJson(false);
        final Table preferenceTable = getTable();
        final Table userPreferenceTable = getUserPreferenceTable();
        final Long orgId = userPreferenceHolder.orgId();
        final Long userId = userPreferenceHolder.getUserId();
        final SelectQuery selectQuery = SelectQuery.get(preferenceTable);
        final Column preferencePKColumn = preferenceTable.getPKColumn();

        selectQuery.addJoinClause(new JoinClause(preferencePKColumn, userPreferenceTable.getColumn(OMCRMUSERPREFERENCE.PREFERENCE_ID)));

        if (Objects.isNull(userId) || !hasUserPreferenceUpdated(orgId, userId)) {
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(userPreferenceTable.getColumn(OMCRMUSERPREFERENCE.IS_UPDATED), false)));
        } else {
            selectQuery.setWhereClause(new WhereClause(Criteria.eq(userPreferenceTable.getColumn(OMCRMUSERPREFERENCE.USER_ID), userId)));
        }
        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);

        dataContainer.getRows(preferenceTable).forEach(preferenceRow -> {
            final JsonObject userPreferenceJson = Json.object((String) preferenceRow.get(OMCRMPREFERENCE.PREFERENCES));

            userPreferenceJson.append(preferenceJson);
            userPreferenceHolder.setId(preferenceRow.getPKValue());
            userPreferenceHolder.addPreferencesAsJson(userPreferenceJson);

            loadPreferencesIntoContainer(isCreateEvent, userPreferenceHolder, dataContainer, targetContainer);
        });
    }

    /**
     * <p>
     * Checks whether the user preference is updated before
     * </p>
     *
     * @param orgId   Represents the organisation Id
     * @param userId  Represents the user Id
     * @return True if user preference is updated before, false otherwise
     */
    private boolean hasUserPreferenceUpdated(final Long orgId, final Long userId) {
        final Table table = getUserPreferenceTable();
        final Row row = getOrgDataStore(orgId).get(table, new WhereClause(Criteria.eq(table.getColumn(OMCRMUSERPREFERENCE.USER_ID), userId)),
                List.of(table.getColumn(OMCRMUSERPREFERENCE.IS_UPDATED))).getRow(table);

        return Objects.isNull(row) ? false : row.get(OMCRMUSERPREFERENCE.IS_UPDATED);
    }

    /**
     * <p>
     * Converts the {@link Row} into {@link UserPreferenceHolder} details.
     * </p>
     *
     * @param userId        Represents the user id.
     * @param preferenceRow Represents the row that holds the information of {@link UserPreferenceHolder}.
     * @return The {@link UserPreferenceHolder}.
     */
    public UserPreferenceHolder convertRowToModel(final long userId, final Row preferenceRow) {
        final UserPreferenceHolder userPreferences = new UserPreferenceHolder();

        loadPreferenceFromRow(userPreferences, preferenceRow);
        userPreferences.setUserId(userId);

        return userPreferences;
    }

    /**
     * {@inheritDoc}
     *
     * @param userPreferenceHolder Represents the {@link UserPreferenceHolder}
     * @param targetContainer      Represents the {@link DataContainer}
     */
    @Override
    public void updateUserPreferencesWithThreadSafety(final UserPreferenceHolder userPreferenceHolder, final DataContainer targetContainer) {
        final Long orgId = userPreferenceHolder.orgId();
        final Long userId = userPreferenceHolder.getUserId();
        final DataContainer userPreferenceDataContainer = getUserPreferenceDataContainer(orgId, userId);

        try {
            DistributedLock distributedLock = null;

            try {
                distributedLock = clusterService.getDistributedLock(TLC_CRM_USER_PREFERENCE_PATH, String.format("%d%s%d", orgId, "_", userId));
                distributedLock.acquire();

                if (userPreferenceDataContainer.isEmpty()) {
                    loadPreferencesIntoContainer(true, userPreferenceHolder, null, targetContainer);
                } else {
                    userPreferenceHolder.setId(userPreferenceDataContainer.getRow(getTable()).getPKValue());
                    loadPreferencesIntoContainer(false, userPreferenceHolder, userPreferenceDataContainer, targetContainer);
                }
                getOrgDataStore(orgId).commitChanges(targetContainer);
            } finally {

                if (Objects.nonNull(distributedLock)) {
                    distributedLock.release();
                }
            }
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "User preference: Error occurred while updating user preferences for Org ID: {}, User ID: {}. Reason: {}", orgId, userId, exception.getMessage());
        }
    }

    /**
     * <p>
     * Gets the user preference source container for the given org id, user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return The {@link DataContainer}
     */
    private DataContainer getUserPreferenceDataContainer(final long orgId, final long userId) {
        final Table preferenceTable = getTable();
        final Table userPreferenceTable = getUserPreferenceTable();
        final SelectQuery selectQuery = SelectQuery.get(userPreferenceTable);

        selectQuery.addSelectClause(preferenceTable);
        selectQuery.addJoinClause(new JoinClause(userPreferenceTable.getColumn(OMCRMUSERPREFERENCE.PREFERENCE_ID), preferenceTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userPreferenceTable.getColumn(OMCRMUSERPREFERENCE.USER_ID), userId)));

        return getOrgDataStore(orgId).get(selectQuery);
    }
}
