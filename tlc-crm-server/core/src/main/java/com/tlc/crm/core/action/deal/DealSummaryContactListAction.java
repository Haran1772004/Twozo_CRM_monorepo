package com.tlc.crm.core.action.deal;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.web.WebAction;

/**
 * <p>
 * Process the contact list action in the deal summary
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
@SourceAuthorizer(subCategory = SubCategory.Type.CONTACTS, validateFieldPermission = true)
@WebAction(path = "/deal/contact/list")
public final class DealSummaryContactListAction extends DealSummaryListAction {

    /**
     * <p>
     * Gets the {@link Source}
     * </p>
     *
     * @return The {@link Source}
     */
    protected Source getSource() {
        return Source.CONTACT;
    }

    /**
     * <p>
     * Gets the {@link ListType}
     * </p>
     *
     * @return The {@link ListType}
     */
    @Override
    protected ListType getListType() {
        return ListType.DEAL_SUMMARY_CONTACT_LIST;
    }
}