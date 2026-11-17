package com.tlc.crm.entity.iam.initiator;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.iam.OrgInitiator;
import com.tlc.crm.common.iam.OrgInitiatorAction;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.entity.marketinglist.manager.MarketingListManager;
import com.tlc.crm.entity.marketinglist.model.MarketingList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Org initiator action responsible for initializing a default marketing list during organization setup
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@OrgInitiator(name = "marketingList")
public class MarketingListOrgInitiator implements OrgInitiatorAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketingListOrgInitiator.class);
    private static final String DEFAULT_MARKETING_LIST_NAME = "Sample List";

    private final MarketingListManager marketingListManager;

    public MarketingListOrgInitiator() {
        marketingListManager = MarketingListManager.getInstance();
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

        LOGGER.info("Org Init Action: Marketing list initialization started. Org ID: {}, User ID: {}", orgId, userId);
        final MarketingList marketingList = new MarketingList();

        marketingList.setOrgId(orgId);
        marketingList.setName(DEFAULT_MARKETING_LIST_NAME);
        marketingList.setCreatedBy(crmUser);
        marketingList.setUpdatedBy(crmUser);

        try {
            marketingListManager.create(marketingList);
            LOGGER.info("Org Init Action: Market list initialization completed. Org ID: {}, User ID: {}", orgId, userId);
        } catch (Throwable throwable) {
            LOGGER.info("Org Init Action: Market list initialization failed. Org ID: {}, User ID: {}, Reason: {}", orgId, userId, throwable.getMessage());
            throw ErrorCode.get(CommonErrorCodes.FAILED_TO_INITIALIZE_ORG, "i18n.marketing.list.org.initialization.failed");
        }
    }
}