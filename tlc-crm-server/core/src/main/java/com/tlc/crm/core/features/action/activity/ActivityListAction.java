package com.tlc.crm.core.features.action.activity;

import com.tlc.crm.common.list.model.ListType;

import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;

import com.tlc.crm.entity.list.EntityListAction;
import com.tlc.crm.entity.list.model.Provider;

import com.tlc.web.WebAction;

/**
 * <p>
 * Retrieves the list of activity
 * </p>
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see EntityListAction
 */
@SourceAuthorizer(relatedApiTypes = SourceAuthorizer.RelatedApiType.ACTIVITY, needRelatedApiHandling = true)
@WebAction(path = "/activity/list")
public class ActivityListAction extends EntityListAction {

    private static final int BEGIN_INDEX = 0;
    private static final int END_INDEX = 1;

    /**
     * {@inheritDoc}
     *
     * @return The provider name.
     */
    @Override
    protected Provider getProvider() {
        return Provider.ACTIVITY_LIST;
    }

    /**
     * <p>
     * Gets the {@link ListType}
     * </p>
     *
     * @return The {@link ListType}
     */
    protected ListType getListType() {
        return ListType.ACTIVITY_LIST;
    }
}
