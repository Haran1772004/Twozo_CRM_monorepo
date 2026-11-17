package com.tlc.crm.common.iam.api.preferences.manager.impl;

import com.tlc.crm.common.iam.api.preferences.manager.OrgPreferenceManager;
import com.tlc.crm.common.iam.api.preferences.manager.UserPreferenceManager;
import com.tlc.crm.common.iam.api.preferences.model.OrgPreference;
import com.tlc.crm.common.iam.api.preferences.model.holder.OrgPreferenceHolder;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.crm.common.sql.resource.OMCRMORGPREFERENCE;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;

import java.util.List;
import java.util.Optional;

/**
 * <p>
 * Implements the {@link OrgPreference} related DB actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class OrgPreferenceManagerImpl extends AbstractPreferenceManager<OrgPreferenceHolder> implements OrgPreferenceManager {

    private final UserPreferenceManager userPreferenceManager;

    private OrgPreferenceManagerImpl() {
        userPreferenceManager = UserPreferenceManager.getInstance();
    }

    private static final class Instance {
        private static final OrgPreferenceManagerImpl PREFERENCE_MANGER = new OrgPreferenceManagerImpl();
    }

    public static OrgPreferenceManagerImpl getInstance() {
        return Instance.PREFERENCE_MANGER;
    }

    /**
     * <p>
     * Fetches the table of the org preference.
     * </p>
     *
     * @return OMCRMOrgPreference table
     */
    private Table getOrgPreferenceTable() {
        return Table.get(OMCRMORGPREFERENCE.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id.
     * @return The organisation {@link OrgPreference} details.
     */
    @Override
    public OrgPreferenceHolder get(final Long orgId) {
        final Table preferenceTable = getTable();
        final Table orgPreferenceTable = getOrgPreferenceTable();
        final SelectQuery selectQuery = SelectQuery.get(orgPreferenceTable);

        selectQuery.addSelectClause(preferenceTable);
        selectQuery.addJoinClause(new JoinClause(orgPreferenceTable.getColumn(OMCRMORGPREFERENCE.PREFERENCE_ID), preferenceTable.getPKColumn()));
        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);
        final Optional<Row> preferenceRow = Optional.ofNullable(dataContainer.getRow(preferenceTable));

        return preferenceRow.map(this::convertRowToModel).orElseGet(() -> null);
    }

    /**
     * <p>
     * Updates the details of the specific {@link OrgPreference}
     * </p>
     *
     * @param orgPreference Represents the {@link OrgPreference} details.
     */
    @Override
    public void updatePreference(final OrgPreference orgPreference) {
        final OrgPreferenceHolder preferenceHolder = get(orgPreference.orgId());

        preferenceHolder.add(orgPreference);
        update(preferenceHolder);
    }

    /**
     * {@inheritDoc}
     *
     * @param isCreateEvent   A flag indicating whether the operation is triggered by a create event.
     * @param orgPreferenceHolder      Represents an {@link OrgPreferenceHolder} details.
     * @param sourceContainer Holds the existing details of the preferences.
     * @param targetContainer Holds the details of the preferences.
     */
    @Override
    public void loadPreferencesIntoContainer(final boolean isCreateEvent, final OrgPreferenceHolder orgPreferenceHolder,
                                             final DataContainer sourceContainer, final DataContainer targetContainer) {
        super.loadPreferencesIntoContainer(isCreateEvent, orgPreferenceHolder, sourceContainer, targetContainer);

        if (isCreateEvent) {
            final Row orgPreferenceRow = new Row(getOrgPreferenceTable());

            orgPreferenceRow.setOrgId(orgPreferenceHolder.orgId());
            orgPreferenceRow.set(OMCRMORGPREFERENCE.PREFERENCE_ID, orgPreferenceHolder.id());

            targetContainer.addNewRow(orgPreferenceRow);
        } else {
            userPreferenceManager.loadPreferencesIntoContainer(false, orgPreferenceHolder.migrateTo(UserPreferenceHolder.class), targetContainer);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param isCreateEvent       A flag indicating whether the operation is triggered by a create event.
     * @param orgPreferenceHolder Represents an {@link OrgPreferenceHolder} details.
     * @param targetContainer     Holds the details of the preferences.
     */
    @Override
    public void loadPreferencesIntoContainer(final boolean isCreateEvent, final OrgPreferenceHolder orgPreferenceHolder,
                                             final DataContainer targetContainer) {
        final DataContainer preferenceDataContainer = getOrgDataStore(orgPreferenceHolder.orgId()).get(getTable(), List.of(orgPreferenceHolder.id()));

        loadPreferencesIntoContainer(isCreateEvent, orgPreferenceHolder, preferenceDataContainer, targetContainer);
    }

    /**
     * <p>
     * Converts the {@link Row} into {@link OrgPreferenceHolder} details
     * </p>
     *
     * @param preferenceRow Represents the row that holds the information of {@link OrgPreference}
     * @return The {@link OrgPreferenceHolder}.
     */
    private OrgPreferenceHolder convertRowToModel(final Row preferenceRow) {
        final OrgPreferenceHolder orgPreferences = new OrgPreferenceHolder();

        loadPreferenceFromRow(orgPreferences, preferenceRow);
        return orgPreferences;
    }
}
