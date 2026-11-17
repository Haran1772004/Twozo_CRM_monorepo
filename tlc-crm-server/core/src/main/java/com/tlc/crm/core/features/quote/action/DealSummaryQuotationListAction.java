package com.tlc.crm.core.features.quote.action;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.core.features.quote.manager.QuotationManager;
import com.tlc.crm.core.list.DealQuotationListSQLInterpreter;
import com.tlc.crm.core.sql.resource.OMQNQUOTATION;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.list.EntityListAction;
import com.tlc.crm.entity.list.EntityListInterpreter;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.entity.list.model.Provider;

import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.WhereClause;

import com.tlc.web.WebAction;

/**
 * <p>
 * Manages the deal quotation list web action. Supports paginated data fetching,
 * field structure retrieval, and total-record-count queries for the
 * quotation list view.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@WebAction(path = "/deal/quote/list")
public class DealSummaryQuotationListAction extends EntityListAction {

    private final QuotationManager quotationManager;

    public DealSummaryQuotationListAction() {
        quotationManager = QuotationManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link DealQuotationListSQLInterpreter} for the given request json and {@link Provider}.
     * </p>
     *
     * @param orgId                   Represents the organisation Id
     * @param userId                  Represents the user Id
     * @param request                 Represents the request {@link JsonObject}
     * @param provider                Represents the {@link Provider}
     * @param joinLoader              Represents the {@link JoinLoader}
     * @param i18NResolutionAdapter   Represents the {@link I18nResolutionAdapter}.
     * @return {@link DealQuotationListSQLInterpreter}
     */
    @Override
    public DealQuotationListSQLInterpreter getEntityListInterpreter(final long orgId, final Long userId, final JsonObject request,
                                                                    final Provider provider, final JoinLoader joinLoader,
                                                                    final I18nResolutionAdapter i18NResolutionAdapter) {
        return new DealQuotationListSQLInterpreter(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Provider getProvider() {
        return Provider.QUOTATION_LIST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ListType getListType() {
        return ListType.DEAL_QUOTATION_LIST;
    }

    /**
     * {@inheritDoc}
     *
     * @param config Represents the {@link FieldConfig.Config}
     * @return True if the field is enabled, false otherwise
     */
    @Override
    protected boolean isFieldEnabled(final FieldConfig.Config config, final Source source) {
        return config.isEnabledForDealQuotationList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void applyFilterCriteria(final long orgId, final long userId, final JsonObject requestJson,
                                       final EntityListInterpreter listInterpreter, final Provider provider) {
        listInterpreter.setWhereClause(new WhereClause(Criteria.eq(quotationManager.getTable().getColumn(OMQNQUOTATION.DEAL_ID),
                requestJson.getLong(FieldKeys.ENTITY_ID.getName()))));
        super.applyFilterCriteria(orgId, userId, requestJson, listInterpreter, provider);
    }
}