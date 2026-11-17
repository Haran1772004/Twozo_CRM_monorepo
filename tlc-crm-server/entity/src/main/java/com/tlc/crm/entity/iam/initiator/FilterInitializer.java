package com.tlc.crm.entity.iam.initiator;

import com.tlc.crm.common.iam.OrgInitiator;
import com.tlc.crm.common.iam.OrgInitiatorAction;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.entity.filter.api.manager.FilterManager;
import com.tlc.crm.entity.filter.api.manager.VisibilityManager;
import com.tlc.crm.entity.filter.api.models.Filter;
import com.tlc.crm.entity.filter.api.models.FilterVisibility;
import com.tlc.crm.entity.service.filter.DefaultFilterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Provides the initializer to execute the create action of the default {@link Filter} for the organisation.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@OrgInitiator(name = "filter")
public class FilterInitializer implements OrgInitiatorAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterInitializer.class);

    private final FilterManager filterManager;
    private final VisibilityManager visibilityManager;
    private final DefaultFilterService filterService;

    public FilterInitializer() {
        filterManager = FilterManager.getInstance();
        visibilityManager = VisibilityManager.getInstance();
        filterService = DefaultFilterService.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(final CrmOrg crmOrg, final CrmUser crmUser) {
        final Long orgId = crmOrg.id();
        final Long userId = crmUser.id();
        final FilterVisibility defaultVisibility = visibilityManager.getDefaultVisibility();

        LOGGER.info("Org Init Action: Filter initialization started. Org ID: {}, User ID: {}", orgId, userId);
        defaultVisibility.setOrgId(orgId);
        visibilityManager.create(defaultVisibility);
        final Map<Long, Long> filterIds = new HashMap<>();

        for (final Filter filter : filterService.getAll()) {
            filter.setOrgId(orgId);
            filter.setFilterVisibility(defaultVisibility);
            filterManager.create(filter);

            if (filter.isRecentlyApplied()) {
                filterIds.put(filter.getFilterProvider().getId(), filter.id());
            }
        }
        filterManager.storeActiveFilters(orgId, userId, filterIds);
        LOGGER.info("Org Init Action: Filter initialization completed. Org ID: {}, User ID: {}", orgId, userId);
    }
}
