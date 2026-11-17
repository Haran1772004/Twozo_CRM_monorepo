package com.tlc.crm.core.features.quote.action;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.core.features.quote.manager.QuotationManager;
import com.tlc.crm.core.list.CompanyQuotationListSQLInterpreter;
import com.tlc.crm.core.sql.resource.OMQNCOMPANYQUOTATION;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.list.EntityListAction;
import com.tlc.crm.entity.list.EntityListInterpreter;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.entity.list.model.Provider;

import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import com.tlc.web.WebAction;

import java.util.List;

/**
 * <p>
 * Manages the company quotation list web action. Supports paginated data fetching,
 * field structure retrieval, and total-record-count queries for the company quotation list view.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@WebAction(path = "/company/quote/list")
public class CompanyQuotationListAction extends EntityListAction {

    private final QuotationManager quotationManager;

    public CompanyQuotationListAction() {
        quotationManager = QuotationManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link CompanyQuotationListSQLInterpreter} for the given request json and {@link Provider}.
     * </p>
     *
     * @param orgId                   Represents the organisation Id
     * @param userId                  Represents the user Id
     * @param request                 Represents the request {@link JsonObject}
     * @param provider                Represents the {@link Provider}
     * @param joinLoader              Represents the {@link JoinLoader}
     * @param i18NResolutionAdapter   Represents the {@link I18nResolutionAdapter}.
     * @return {@link CompanyQuotationListSQLInterpreter}
     */
    @Override
    public CompanyQuotationListSQLInterpreter getEntityListInterpreter(final long orgId, final Long userId, final JsonObject request,
                                                                       final Provider provider, final JoinLoader joinLoader,
                                                                       final I18nResolutionAdapter i18NResolutionAdapter) {
        return new CompanyQuotationListSQLInterpreter(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
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
        return ListType.COMPANY_QUOTATION_LIST;
    }

    /**
     * {@inheritDoc}
     *
     * @param config Represents the {@link FieldConfig.Config}
     * @return True if the field is enabled, false otherwise
     */
    @Override
    protected boolean isFieldEnabled(final FieldConfig.Config config, final Source source) {
        return config.isEnabledForCompanyQuotationList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void applyFilterCriteria(final long orgId, final long userId, final JsonObject requestJson,
                                       final EntityListInterpreter listInterpreter, final Provider provider) {
        final Table companyQuotationTable = quotationManager.getCompanyQuotationTable();

        listInterpreter.setWhereClause(new WhereClause(Criteria.eq(companyQuotationTable.getColumn(OMQNCOMPANYQUOTATION.COMPANY_ID),
                requestJson.getLong(FieldKeys.ENTITY_ID.getName()))));
        listInterpreter.addJoinClause(List.of(new JoinClause(quotationManager.getTable().getPKColumn(),
                companyQuotationTable.getColumn(OMQNCOMPANYQUOTATION.QUOTATION_ID), JoinClause.JoinType.LEFT)));
        super.applyFilterCriteria(orgId, userId, requestJson, listInterpreter, provider);
    }
}