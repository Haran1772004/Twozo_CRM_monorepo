package com.tlc.crm.core.features.action.quota.list;

import com.tlc.crm.common.list.model.ListRequestData;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.security.permission.annotation.authorizer.QuotaAuthorizer;
import com.tlc.web.WebAction;

/**
 * Manages the team view quota list related web actions
 *
 * @author kishore J
 * @version 1.0
 */
@QuotaAuthorizer(validateBasicAPIs = true)
@WebAction(path = "/quota/hierarchy/view/list")
public class QuotaHierarchyViewList extends QuotaListAction {

    private static final String NAME = "hierarchyView";

    /**
     * {@inheritDoc}
     *
     * @param listRequestData Represents the {@link ListRequestData}
     * @return The {@link QuotaSQLListInterpreter}
     */
    @Override
    protected QuotaListInterpreter getListInterpreter(final ListRequestData listRequestData) {
        return new QuotaHierarchyViewSQLListInterpreter(listRequestData, getProvider());
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Provider}
     */
    @Override
    protected Provider getProvider() {
        return Provider.QUOTA_HIERARCHY_VIEW_LIST;
    }

    /**
     * {@inheritDoc}
     *
     * @return the list view name
     */
    @Override
    protected String getName() {
        return NAME;
    }
}
