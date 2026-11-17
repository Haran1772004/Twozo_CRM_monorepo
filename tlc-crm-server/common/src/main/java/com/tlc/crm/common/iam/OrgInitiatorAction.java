package com.tlc.crm.common.iam;

import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.internal.CrmUserEntity;

/**
 * <p>
 * A action initialize the crm org and crm user
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface OrgInitiatorAction {

    /**
     * <p>
     * Executes the create action for {@link CrmOrg} and {@link CrmUser}
     * </p>
     *
     * @param crmOrg  Represents the {@link CrmOrg}
     * @param crmUser Represents the {@link CrmUserEntity}
     */
    void execute(final CrmOrg crmOrg, final CrmUser crmUser);
}
