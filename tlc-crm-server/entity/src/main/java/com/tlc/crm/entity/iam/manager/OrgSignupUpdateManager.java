package com.tlc.crm.entity.iam.manager;

import com.tlc.crm.common.iam.api.preferences.manager.OrgPreferenceManager;
import com.tlc.crm.common.iam.api.preferences.manager.PreferenceManager;
import com.tlc.crm.common.iam.api.preferences.manager.UserPreferenceManager;
import com.tlc.crm.common.iam.api.preferences.model.holder.OrgPreferenceHolder;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.crm.common.iam.internal.manager.OrganizationManager;

import com.tlc.crm.entity.currency.api.manager.BaseCurrencyManager;
import com.tlc.crm.entity.currency.api.model.BaseCurrency;

import com.tlc.sql.common.api.DataContainer;

/**
 * Responsible for updating organization-related settings during the signup process.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class OrgSignupUpdateManager {

    private final BaseCurrencyManager baseCurrencyManager;
    private final PreferenceManager<OrgPreferenceHolder> orgPreferenceManager;
    private final UserPreferenceManager userPreferenceManager;
    private final OrganizationManager organizationManager;

    private OrgSignupUpdateManager() {
        this.baseCurrencyManager = BaseCurrencyManager.getInstance();
        this.orgPreferenceManager = OrgPreferenceManager.getInstance();
        this.userPreferenceManager = UserPreferenceManager.getInstance();
        this.organizationManager = OrganizationManager.getInstance();
    }

    private static class Instance {
        private static final OrgSignupUpdateManager INSTANCE = new OrgSignupUpdateManager();
    }

    /**
     * Gets the instance of {@code OrgSignupUpdateManager}.
     *
     * @return Instance of {@code OrgSignupUpdateManager}.
     */
    public static OrgSignupUpdateManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Updates organization preferences and base currency during signup.
     *
     * @param orgId               The unique identifier of the organization.
     * @param orgPreferenceHolder An object containing organization preference settings.
     * @param baseCurrency        The base currency information to be set for the organization.
     */
    public void update(final long orgId, final OrgPreferenceHolder orgPreferenceHolder,
                       final UserPreferenceHolder userPreferenceHolder, final BaseCurrency baseCurrency) {
        final DataContainer targetContainer = DataContainer.create();

        baseCurrencyManager.createBaseCurrency(orgId, baseCurrency, targetContainer);
        orgPreferenceManager.loadPreferencesIntoContainer(true, orgPreferenceHolder, null, targetContainer);
        userPreferenceManager.updateUserPreferencesWithThreadSafety(userPreferenceHolder, targetContainer);
    }
}
