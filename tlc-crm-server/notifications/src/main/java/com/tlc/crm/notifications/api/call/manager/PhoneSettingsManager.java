package com.tlc.crm.notifications.api.call.manager;

import com.tlc.crm.notifications.api.call.manager.impl.PhoneSettingsManagerImpl;
import com.tlc.crm.notifications.api.call.model.PhoneSettings;

import com.tlc.sql.common.api.dml.Table;

/**
 * An interface to manage database actions related to phone settings
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface PhoneSettingsManager {

    /**
     * Gets the instance of {@link PhoneSettingsManager}
     *
     * @return the instance of {@link PhoneSettingsManager}
     */
    static PhoneSettingsManager getInstance() {
        return PhoneSettingsManagerImpl.getInstance();
    }

    /**
     * Gets the phone settings table
     *
     * @return the phone settings table
     */
    Table getTable();

    /**
     * <p>
     * Gets the {@link PhoneSettings} for the provided org
     * </p>
     *
     * @param orgId Represents the org id
     * @return the {@link PhoneSettings}
     */
    PhoneSettings get(final Long orgId);

    /**
     * <p>
     * Updates the {@link PhoneSettings}
     * </p>
     *
     * @param userId        Represents the user id
     * @param phoneSettings Represents the {@link PhoneSettings} to be updated
     */
    void update(final Long userId, final PhoneSettings phoneSettings);

    /**
     * <p>
     * Checks whether the calling service exists or not
     * </p>
     *
     * @param callingService Represents the calling service
     * @return true, if it exists, false otherwise
     */
    boolean exists(final PhoneSettings.CallingService callingService);
}