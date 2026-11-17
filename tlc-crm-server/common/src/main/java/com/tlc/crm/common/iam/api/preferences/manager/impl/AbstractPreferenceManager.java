package com.tlc.crm.common.iam.api.preferences.manager.impl;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.preferences.manager.PreferenceManager;
import com.tlc.crm.common.iam.api.preferences.model.Preference;
import com.tlc.crm.common.iam.api.preferences.model.holder.PreferenceHolder;
import com.tlc.crm.common.sql.resource.OMCRMPREFERENCE;

import com.tlc.sql.common.SQLService;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.util.Objects;

/**
 * <p>
 * Implements the {@link Preference} related DB actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class AbstractPreferenceManager<P extends PreferenceHolder<?, ?>> implements PreferenceManager<P> {

    private final SQLService sqlService;

    protected AbstractPreferenceManager() {
        sqlService = SqlServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     *
     * @return OMCRMPreference table.
     */
    @Override
    public Table getTable() {
        return Table.get(OMCRMPREFERENCE.TABLE);
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore}
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The {@link OrgDataStore}
     */
    protected OrgDataStore getOrgDataStore(final long orgId) {
        return sqlService.getOrgDataStore(orgId);
    }

    /**
     * <p>
     * Updates the details of the {@link Preference}
     * </p>
     *
     * @param preferenceHolder Represents the {@link Preference} details.
     */
    @Override
    public void update(final P preferenceHolder) {
        final DataContainer targetContainer = DataContainer.create();

        loadPreferencesIntoContainer(false, preferenceHolder, targetContainer);
        getOrgDataStore(preferenceHolder.orgId()).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the preference details into {@link DataContainer}.
     * </p>
     *
     * @param isCreateEvent   A flag indicating whether the operation is triggered by a create event.
     * @param preference      Represents an {@link Preference} details.
     * @param sourceContainer Holds the existing details of the preferences.
     * @param targetContainer Holds the details of the preferences.
     */
    @Override
    public void loadPreferencesIntoContainer(final boolean isCreateEvent, final P preference,
                                             final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table table = getTable();
        final Row preferenceRow = isCreateEvent ? new Row(table) : sourceContainer.getRow(table, preference.id());

        if (Objects.nonNull(preferenceRow)) {
            final JsonObject preferenceJson;

            if (preferenceRow.isNewRow()) {
                preferenceJson = preference.toJson(false);

                preferenceRow.setOrgId(preference.orgId());
                preference.setId(preferenceRow.getPKValue());
            } else {
                preferenceJson = Json.object((String) preferenceRow.get(OMCRMPREFERENCE.PREFERENCES));

                preferenceJson.append(preference.toJson(false));
            }
            preferenceRow.set(OMCRMPREFERENCE.PREFERENCES, preferenceJson.toString());

            targetContainer.updateRow(preferenceRow);
        }
    }

    /**
     * <p>
     * Loads the {@link PreferenceHolder} from the {@link Row}.
     * </p>
     *
     * @param preferenceHolder   Holds the details of the preferenceHolder.
     * @param preferenceRow Represents the row that holds the information of {@link Preference}
     */
    protected final void loadPreferenceFromRow(final P preferenceHolder, final Row preferenceRow) {
        preferenceHolder.setId(preferenceRow.getPKValue());
        preferenceHolder.setOrgId(preferenceRow.getOrgId());
        preferenceHolder.addPreferencesAsJson(Json.object(preferenceRow.get(OMCRMPREFERENCE.PREFERENCES).toString()));
    }
}
