package com.tlc.crm.entity.iam.forecastcategory;

import com.tlc.crm.common.iam.OrgInitiator;
import com.tlc.crm.common.iam.OrgInitiatorAction;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.entity.forecastcategory.manager.ForecastCategoryManager;
import com.tlc.crm.entity.forecastcategory.model.DefaultForecastCategory;
import com.tlc.crm.entity.forecastcategory.model.ForecastCategory;
import com.tlc.i18n.I18nKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * <p>
 * Org initiator action responsible for initializing default forecast categories during organization setup or onboarding
 * </p>
 *
 * @author kishore J
 * @version 1.0
 */
@OrgInitiator(name = "forecastCategory")
public class ForecastCategoryOrgInitiator implements OrgInitiatorAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForecastCategoryOrgInitiator.class);

    private final ForecastCategoryManager forecastCategoryManager;

    public ForecastCategoryOrgInitiator() {
        forecastCategoryManager = ForecastCategoryManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmOrg  Represents the {@link CrmOrg}
     * @param crmUser Represents the {@link CrmUser}
     */
    @Override
    public void execute(final CrmOrg crmOrg, final CrmUser crmUser) {
        final DefaultForecastCategory[] defaultForecastCategories = DefaultForecastCategory.values();
        final Long orgId = crmOrg.id();
        final Collection<ForecastCategory> forecastCategories = new ArrayList<>(defaultForecastCategories.length);

        for (final DefaultForecastCategory defaultForecastCategory : defaultForecastCategories) {
            final ForecastCategory forecastCategory = new ForecastCategory(defaultForecastCategory.buildConfig());

            forecastCategory.setOrgId(orgId);
            forecastCategory.setCreatedBy(crmUser);
            forecastCategory.setUpdatedBy(crmUser);
            forecastCategory.setName(defaultForecastCategory.getName());
            forecastCategory.setDescription(defaultForecastCategory.getDescription());
            forecastCategory.setDisplayName(defaultForecastCategory.getDisplayName());
            forecastCategory.setDefault(defaultForecastCategory.isDefault());

            forecastCategories.add(forecastCategory);
        }
        forecastCategoryManager.create(orgId, forecastCategories);
        LOGGER.info("Org Init Action: Default forecastCategory initialization completed. Org ID: {}, User ID: {}", orgId, crmUser.id());
    }
}
