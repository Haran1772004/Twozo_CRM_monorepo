package com.tlc.crm.common.iam.api.preferences.manager;

import com.tlc.crm.common.iam.api.preferences.manager.impl.UserPreferenceManagerImpl;
import com.tlc.crm.common.iam.api.preferences.model.UserPreference;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;

/**
 * <p>
 * Manages the user {@link UserPreference} related DB actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface UserPreferenceManager extends PreferenceManager<UserPreferenceHolder> {

    static UserPreferenceManager getInstance() {
        return UserPreferenceManagerImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link UserPreference} from the data base.
     * </p>
     *
     * @param orgId  Represents an organization id.
     * @param userId Represents the user id.
     * @return The {@link UserPreference} details.
     */
    UserPreferenceHolder get(final long orgId, final long userId);

    /**
     * <p>
     * Fetches the user's id
     * </p>
     *
     * @param orgId        Represents an organization id.
     * @param preferenceId Represents the {@link UserPreference} id.
     * @return The {@link UserPreference} details.
     */
    Long getUserId(final long orgId, final long preferenceId);

    /**
     * <p>
     * Updates the details of the specific {@link UserPreference}
     * </p>
     *
     * @param userPreference Represents the {@link UserPreference} details.
     */
    void updatePreference(final UserPreference userPreference);

    /**
     * <p>
     * Converts the {@link Row} into {@link UserPreferenceHolder} details.
     * </p>
     *
     * @param userId        Represents the user id.
     * @param preferenceRow Represents the row that holds the information of {@link UserPreferenceHolder}.
     * @return The {@link UserPreferenceHolder}.
     */
    UserPreferenceHolder convertRowToModel(final long userId, final Row preferenceRow);

    /**
     * <p>
     * Updates the user preferences for a given organization and user using a distributed lock
     * to prevent concurrent modifications
     * </p>
     *
     * @param userPreferenceHolder Represents the {@link UserPreferenceHolder}
     * @param targetContainer      The target {@link DataContainer} where the preferences should be applied
     */
    void updateUserPreferencesWithThreadSafety(final UserPreferenceHolder userPreferenceHolder, final DataContainer targetContainer);
}
