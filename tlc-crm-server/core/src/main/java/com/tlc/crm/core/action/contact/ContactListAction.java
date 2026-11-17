package com.tlc.crm.core.action.contact;

import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;

import com.tlc.crm.entity.list.EntityListAction;
import com.tlc.crm.entity.list.model.Provider;

import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.web.WebAction;

/**
 * @author Abishek
 * @version 1.0
 */
@SourceAuthorizer(subCategory = SubCategory.Type.CONTACTS, validateFieldPermission = true)
@WebAction(path = "/contact/list")
public class ContactListAction extends EntityListAction {

    /**
     * {@inheritDoc}
     *
     * @return The provider name.
     */
    @Override
    protected Provider getProvider() {
        return Provider.CONTACT_LIST;
    }

    /**
     * <p>
     * Gets the {@link ListType}
     * </p>
     *
     * @return The {@link ListType}
     */
    protected ListType getListType() {
        return ListType.CONTACT_LIST;
    }
}
