package com.tlc.crm.core.action.contact;

import com.tlc.crm.common.list.ListInterpreter;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.web.WebAction;

/**
 * <p>
 * Process the deal list action in the contact summary
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
@SourceAuthorizer(subCategory = SubCategory.Type.DEALS, validateFieldPermission = true)
@WebAction(path = "/contact/deal/list")
public final class ContactSummaryDealListAction extends ContactSummaryListAction {

    /**
     * <p>
     * Gets the {@link Source}
     * </p>
     *
     * @return The {@link Source}
     */
    protected Source getSource() {
        return Source.DEAL;
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
        return ListType.CONTACT_SUMMARY_DEAL_LIST;
    }

    /**
     * <p>
     * Applies the default criteria for the entity list action
     * </p>
     *
     * @param listInterpreter Represents the {@link ListInterpreter}
     */
    @Override
    protected void applyDefaultCriteria(final ListInterpreter listInterpreter) {
        listInterpreter.setWhereClause(new WhereClause(Criteria.eq(dealManager.getTable().getColumn(MDODEAL.STATUS), Deal.Status.ACTIVE.getId())));
    }
}