package com.tlc.crm.core.features.invoice.action;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.core.features.invoice.manager.InvoiceManager;
import com.tlc.crm.core.features.invoice.manager.InvoiceManagerImpl;
import com.tlc.crm.core.list.ContactInvoiceListSQLInterpreter;
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
 * Manages the contact invoice list web action.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@WebAction(path = "/contact/invoice/list")
public class ContactInvoiceListAction extends EntityListAction {

    private final InvoiceManager invoiceManager;

    /**
     * <p>
     * Initializes the contact invoice list action.
     * </p>
     */
    public ContactInvoiceListAction() {
        this.invoiceManager = InvoiceManagerImpl.getInstance();
    }

    /**
     * <p>
     * Creates the SQL interpreter for the contact invoice list.
     * </p>
     *
     * @param orgId                 Represents the organization ID.
     * @param userId                Represents the user ID.
     * @param request               Represents the list request.
     * @param provider              Represents the list provider.
     * @param joinLoader            Represents the join loader.
     * @param i18NResolutionAdapter Represents the i18n resolution adapter.
     * @return The contact invoice list SQL interpreter.
     */
    @Override
    public ContactInvoiceListSQLInterpreter getEntityListInterpreter(final long orgId, final Long userId, final JsonObject request,
                                                                     final Provider provider, final JoinLoader joinLoader,
                                                                     final I18nResolutionAdapter i18NResolutionAdapter) {
        return new ContactInvoiceListSQLInterpreter(userId, orgId, provider, joinLoader, i18NResolutionAdapter,
                maxFieldSelectionLimit, maxFilterCriteriaLimit);
    }

    /**
     * <p>
     * Returns the provider used for the contact invoice list.
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
     * Returns the list type used for the contact invoice list.
     * </p>
     *
     * @return The contact invoice list type.
     */
    @Override
    protected ListType getListType() {
        return ListType.CONTACT_INVOICE_LIST;
    }

    /**
     * <p>
     * Determines whether the field is enabled for the contact invoice list.
     * </p>
     *
     * @param config Represents the field configuration.
     * @param source Represents the field source.
     * @return {@code true} if the field is enabled; otherwise, {@code false}.
     */
    @Override
    protected boolean isFieldEnabled(final FieldConfig.Config config, final Source source) {
        return config.isEnabledForContactInvoiceList();
    }

    /**
     * <p>
     * Applies the contact filter criteria to the invoice list.
     * </p>
     *
     * @param orgId           Represents the organization ID.
     * @param userId          Represents the user ID.
     * @param requestJson     Represents the request JSON.
     * @param listInterpreter Represents the entity list interpreter.
     * @param provider        Represents the list provider.
     */
    @Override
    protected void applyFilterCriteria(final long orgId, final long userId, final JsonObject requestJson,
                                       final EntityListInterpreter listInterpreter, final Provider provider) {
        listInterpreter.setWhereClause(new WhereClause(Criteria.eq(invoiceManager.getTable().getColumn(OMIVINVOICE.CONTACT_ID),
                requestJson.getLong(FieldKeys.ENTITY_ID.getName()))));

        super.applyFilterCriteria(orgId, userId, requestJson, listInterpreter, provider);
    }
}