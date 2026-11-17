package com.tlc.crm.core.iam;

import com.tlc.crm.common.iam.OrgInitiator;
import com.tlc.crm.common.iam.OrgInitiatorAction;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.manager.UserPreferenceManager;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.crm.core.api.iam.model.UserCompanyPreference;
import com.tlc.crm.core.api.iam.model.UserContactPreference;
import com.tlc.sql.common.api.DataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Org initiator action responsible for initializing default preferences during organization setup or onboarding
 * </p>
 *
 * @author kishore J
 * @version 1.0
 */
@OrgInitiator(name = "preferenceInitiator")
public class PreferenceInitiatorAction implements OrgInitiatorAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompanyOrgInitiator.class);

    private final UserPreferenceManager userPreferenceManager;

    public PreferenceInitiatorAction() {
        userPreferenceManager = UserPreferenceManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmOrg  Represents the {@link CrmOrg}
     * @param crmUser Represents the {@link CrmUser}
     */
    @Override
    public void execute(final CrmOrg crmOrg, final CrmUser crmUser) {
        final Long orgId = crmOrg.id();
        final Long userId = crmUser.id();

        LOGGER.info("Org Init Action: Preference initialization started. Org ID: {}, User ID: {}", orgId, userId);
        final UserPreferenceHolder userPreferenceHolder = new UserPreferenceHolder();
        final UserContactPreference userContactPreference = new UserContactPreference();
        final UserCompanyPreference userCompanyPreference = new UserCompanyPreference();

        userContactPreference.setDuplicateCheckEnabled(true);
        userCompanyPreference.setDuplicateCheckEnabled(true);
        userPreferenceHolder.add(userCompanyPreference);
        userPreferenceHolder.add(userContactPreference);

        userPreferenceHolder.setUserId(userId);
        userPreferenceHolder.setOrgId(orgId);

        userPreferenceManager.updateUserPreferencesWithThreadSafety(userPreferenceHolder, DataContainer.create());
        LOGGER.info("Org Init Action: Preference initialization completed. Org ID: {}, User ID: {}", orgId, userId);
    }
}
