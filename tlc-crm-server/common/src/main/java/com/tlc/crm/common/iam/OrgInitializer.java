package com.tlc.crm.common.iam;

import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Represents an action performed by the organization initiator.
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@OrgInitiator(name = "crm")
public class OrgInitializer implements OrgInitiatorAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrgInitializer.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(final CrmOrg crmOrg, final CrmUser crmUser) {
        LOGGER.info("Org Init Action: Common initialization completed OrgId : {}, UserId : {}", crmOrg.getIamOrgId(), crmUser.getIamId());
        // TODO Implement the organisation initialization activities
    }
}
