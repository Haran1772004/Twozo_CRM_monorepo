package com.tlc.crm.core.action.contact;

import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.entity.list.EntityListAction;
import com.tlc.crm.entity.list.model.Provider;

import com.tlc.web.WebAction;

/**
 * Manages {@link EntityListAction} related actions
 *
 * @author kowsalya SP
 * @version 1.0
 */
@SourceAuthorizer(subCategory = SubCategory.Type.COMPANIES, validateFieldPermission = true)
@WebAction(path = "/company/list")
public class CompanyListAction extends EntityListAction {

    /**
     * {@inheritDoc}
     *
     * @return {@link Provider}
     */
    @Override
    protected Provider getProvider() {
        return Provider.COMPANY_LIST;
    }

    /**
     * <p>
     * Gets the {@link ListType}
     * </p>
     *
     * @return The {@link ListType}
     */
    protected ListType getListType() {
        return ListType.COMPANY_LIST;
    }
}

