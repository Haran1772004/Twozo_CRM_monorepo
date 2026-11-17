package com.tlc.crm.core.action.contact;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.web.WebAction;

/**
 * <p>
 * Process the company list action in the contact summary
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
@SourceAuthorizer(subCategory = SubCategory.Type.COMPANIES, validateFieldPermission = true)
@WebAction(path = "/contact/company/list")
public final class ContactSummaryCompanyListAction extends ContactSummaryListAction {

    /**
     * <p>
     * Gets the {@link Source}
     * </p>
     *
     * @return The {@link Source}
     */
    protected Source getSource() {
        return Source.COMPANY;
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
        return ListType.CONTACT_SUMMARY_COMPANY_LIST;
    }
}