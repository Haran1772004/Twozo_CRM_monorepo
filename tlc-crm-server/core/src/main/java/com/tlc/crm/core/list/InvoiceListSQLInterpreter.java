package com.tlc.crm.core.list;

import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.common.list.model.ConfiguredField;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.features.invoice.fields.InvoiceFields;
import com.tlc.crm.core.features.invoice.manager.InvoiceManager;
import com.tlc.crm.core.sql.resource.MPOPRODUCT;
import com.tlc.crm.core.sql.resource.OMIVINVOICELINEITEMS;
import com.tlc.crm.entity.list.EntitySQLListInterpreter;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.Table;

import java.util.List;

import static com.tlc.sql.common.api.dml.JoinClause.JoinType.LEFT;

/**
 * <p>
 * Base interpreter for all invoice list variants (contact, deal, company).
 * </p>
 *
 * @author  Mohammed Asif J.
 * @version 1.0
 */
public abstract class InvoiceListSQLInterpreter extends EntitySQLListInterpreter {

    private final InvoiceManager invoiceManager;
    private final ProductManager productManager;

    protected InvoiceListSQLInterpreter(final Long userId, final Long orgId, final Provider provider,
                                        final JoinLoader joinLoader, final I18nResolutionAdapter i18NResolutionAdapter,
                                        final int maxFieldSelectionLimit, final int maxFilterCriteriaLimit) {
        super(userId, orgId, provider, joinLoader, i18NResolutionAdapter, maxFieldSelectionLimit, maxFilterCriteriaLimit);
        this.invoiceManager = InvoiceManager.getInstance();
        this.productManager = ProductManager.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectClause(final ConfiguredField configuredField) {
        if (configuredField.isEnabled() && InvoiceFields.INVOICE_LINE_ITEMS.equals(configuredField.fieldName())) {
            final Table lineItemTable = invoiceManager.getInvoiceLineItemsTable();
            final Table productTable = productManager.getTable();

            selectQuery.addJoinClause(List.of(
                    new JoinClause(invoiceManager.getTable().getPKColumn(), lineItemTable.getColumn(OMIVINVOICELINEITEMS.INVOICE_ID), LEFT),
                    new JoinClause(lineItemTable.getColumn(OMIVINVOICELINEITEMS.PRODUCT_ID), productTable.getColumn(MPOPRODUCT.ID), LEFT)
            ));

            selectQuery.addSelectClause(List.of(
                    lineItemTable.getColumn(OMIVINVOICELINEITEMS.PRODUCT_ID, InvoiceFields.PRODUCT_ID),
                    lineItemTable.getColumn(OMIVINVOICELINEITEMS.AMOUNT, InvoiceFields.AMOUNT),
                    productTable.getColumn(MPOPRODUCT.NAME, InvoiceFields.PRODUCT_NAME)
            ));
            return;
        }

        super.selectClause(configuredField);
    }
}