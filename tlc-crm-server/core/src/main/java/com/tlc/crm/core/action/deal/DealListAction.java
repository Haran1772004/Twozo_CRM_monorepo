package com.tlc.crm.core.action.deal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;

import com.tlc.crm.common.list.ListInterpreter;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.entity.list.EntityListAction;
import com.tlc.crm.entity.list.model.Provider;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.web.WebAction;

/**
 * @author Sundar
 * @version 1.0
 */
@SourceAuthorizer(subCategory = SubCategory.Type.DEALS, validateFieldPermission = true)
@WebAction(path = "/deal/list")
public class DealListAction extends EntityListAction {

    private final DealManager dealManager;
    private final PipelineManager pipelineManager;

    public DealListAction() {
        dealManager = DealManager.getInstance();
        pipelineManager = PipelineManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The provider name.
     */
    @Override
    protected Provider getProvider() {
        return Provider.DEAL_LIST;
    }

    /**
     * <p>
     * Applies filter criteria for a deal
     * </p>
     *
     * @param listInterpreter Represents {@link ListInterpreter}.
     * @param filterCriteria  Hold's details about criteria to filter.
     */
    @Override
    protected void applyCustomCriteria(final ListInterpreter listInterpreter, final JsonObject filterCriteria) {
        final Table dealTable = dealManager.getTable();

        if (filterCriteria.containsKey(ListActionFields.PIPELINE_ID)) {
            final long pipelineId = filterCriteria.getLong(ListActionFields.PIPELINE_ID);
            final CrmUser crmUser = LoggedInUserInfo.getUser();
            final long orgId = crmUser.orgId();

            if (!pipelineManager.exists(orgId, pipelineId)) {
                throw ErrorCode.get(DealErrorCodes.PIPELINE_NOT_FOUND, "i18n.pipeline.not.found");
            }
            pipelineManager.loadUserPreferredPipeline(orgId, crmUser.id(), pipelineId);
            listInterpreter.setWhereClause(new WhereClause(Criteria.eq(
                    dealTable.getColumn(MDODEAL.PIPELINE_ID), pipelineId)));
        }
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

    /**
     * <p>
     * Gets the {@link ListType}
     * </p>
     *
     * @return The {@link ListType}
     */
    protected ListType getListType() {
        return ListType.DEAL_LIST;
    }
}
