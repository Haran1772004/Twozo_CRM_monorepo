package com.tlc.crm.core.features.invoice.action;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.core.features.invoice.manager.InvoiceManager;
import com.tlc.crm.core.list.DealInvoiceListSQLInterpreter;
import com.tlc.crm.core.sql.resource.OMIVINVOICE;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.list.EntityListAction;
import com.tlc.crm.entity.list.EntityListInterpreter;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.web.WebAction;

/**
 * <p>
 * Manages the deal invoice list web action.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@WebAction(path = "/deal/invoice/list")
public class DealInvoiceListAction extends EntityListAction {

    private final InvoiceManager invoiceManager;

    /**
     * <p>
     * Initializes the deal invoice list action with the invoice manager.
     * </p>
     */
    public DealInvoiceListAction() {
        this.invoiceManager = InvoiceManager.getInstance();
    }

    /**
     * <p>
     * Creates the SQL interpreter used to process the deal invoice list request.
     * </p>
     *
     * @param orgId                   Represents the organization ID.
     * @param userId                  Represents the user ID.
     * @param request                 Represents the list request.
     * @param provider                Represents the entity list provider.
     * @param joinLoader              Represents the join loader.
     * @param i18NResolutionAdapter   Represents the i18n resolution adapter.
     * @return The deal invoice list SQL interpreter.
     */
    @Override
    public DealInvoiceListSQLInterpreter getEntityListInterpreter(final long orgId, final Long userId, final JsonObject request,
                                                                  final Provider provider, final JoinLoader joinLoader,
                                                                  final I18nResolutionAdapter i18NResolutionAdapter) {
        return new DealInvoiceListSQLInterpreter(userId, orgId, provider, joinLoader, i18NResolutionAdapter,
                maxFieldSelectionLimit, maxFilterCriteriaLimit);
    }

    /**
     * <p>
     * Returns the provider used for the invoice list.
     * </p>
     *
     * @return The invoice list provider.
     */
    @Override
    protected Provider getProvider() {
        return Provider.INVOICE_LIST;
    }

    /**
     * <p>
     * Returns the list type used for the deal invoice list.
     * </p>
     *
     * @return The deal invoice list type.
     */
    @Override
    protected ListType getListType() {
        return ListType.DEAL_INVOICE_LIST;
    }

    /**
     * <p>
     * Determines whether the field is enabled for the deal invoice list.
     * </p>
     *
     * @param config Represents the field configuration.
     * @param source Represents the field source.
     * @return {@code true} if the field is enabled for the deal invoice list; otherwise {@code false}.
     */
    @Override
    protected boolean isFieldEnabled(final FieldConfig.Config config, final Source source) {
        return config.isEnabledForDealInvoiceList();
    }

    /**
     * <p>
     * Applies the deal-specific filter criteria to the invoice list.
     * </p>
     *
     * @param orgId            Represents the organization ID.
     * @param userId           Represents the user ID.
     * @param requestJson      Represents the list request JSON.
     * @param listInterpreter  Represents the entity list interpreter.
     * @param provider         Represents the entity list provider.
     */
    @Override
    protected void applyFilterCriteria(final long orgId, final long userId, final JsonObject requestJson,
                                       final EntityListInterpreter listInterpreter, final Provider provider) {
        listInterpreter.setWhereClause(new WhereClause(Criteria.eq(invoiceManager.getTable().getColumn(OMIVINVOICE.DEAL_ID),
                requestJson.getLong(FieldKeys.ENTITY_ID.getName()))));

        super.applyFilterCriteria(orgId, userId, requestJson, listInterpreter, provider);
    }
}