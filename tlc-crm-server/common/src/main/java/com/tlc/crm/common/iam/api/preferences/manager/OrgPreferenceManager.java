package com.tlc.crm.common.iam.api.preferences.manager;

import com.tlc.crm.common.iam.api.preferences.manager.impl.OrgPreferenceManagerImpl;
import com.tlc.crm.common.iam.api.preferences.model.OrgPreference;
import com.tlc.crm.common.iam.api.preferences.model.holder.OrgPreferenceHolder;

/**
 * <p>
 * Manages the organisation {@link OrgPreference} related DB actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface OrgPreferenceManager extends PreferenceManager<OrgPreferenceHolder> {

    static OrgPreferenceManager getInstance() {
        return OrgPreferenceManagerImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link OrgPreference} from the DB.
     * </p>
     *
     * @param orgId Represents an organization id.
     * @return The {@link OrgPreference} details.
     */
    OrgPreferenceHolder get(final Long orgId);

    /**
     * <p>
     * Updates the details of the specific {@link OrgPreference}
     * </p>
     *
     * @param orgPreference Represents the {@link OrgPreference} details.
     */
    void updatePreference(final OrgPreference orgPreference);
}
