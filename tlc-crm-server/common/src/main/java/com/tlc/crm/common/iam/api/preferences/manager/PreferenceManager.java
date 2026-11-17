package com.tlc.crm.common.iam.api.preferences.manager;

import com.tlc.crm.common.iam.api.preferences.model.Preference;
import com.tlc.crm.common.iam.api.preferences.model.holder.PreferenceHolder;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Table;

/**
 * <p>
 * Manages the {@link Preference} related DB actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PreferenceManager<P extends PreferenceHolder<?, ?>> {

    /**
     * <p>
     * Fetches the table of the preference.
     * </p>
     *
     * @return OMCRMPreference table.
     */
    Table getTable();

    /**
     * <p>
     * Updates the details of the {@link Preference}
     * </p>
     *
     * @param preferenceHolder Represents the {@link Preference} details.
     */
    void update(final P preferenceHolder);

    /**
     * <p>
     * Loads the preferences details into {@link DataContainer}.
     * </p>
     *
     * @param isCreateEvent    A flag indicating whether the operation is triggered by a create event.
     * @param preferenceHolder Represents an {@link Preference} details.
     * @param sourceContainer  Holds the existing details of the preferences.
     * @param targetContainer  Holds the details of the preferences.
     */
    void loadPreferencesIntoContainer(final boolean isCreateEvent, final P preferenceHolder, final DataContainer sourceContainer,
                                      final DataContainer targetContainer);

    /**
     * <p>
     * Loads the preference details into {@link DataContainer}.
     * </p>
     *
     * @param isCreateEvent    A flag indicating whether the operation is triggered by a create event.
     * @param preferenceHolder Represents an {@link Preference} details.
     * @param targetContainer  Holds the details of the preferences.
     */
    void loadPreferencesIntoContainer(final boolean isCreateEvent, final P preferenceHolder, final DataContainer targetContainer);
}
