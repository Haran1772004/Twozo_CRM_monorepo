package com.tlc.crm.core.features.invoice.action;

import com.tlc.crm.core.features.sales.template.AbstractSalesDocumentRetrievalManagement;
import com.tlc.crm.core.features.invoice.fields.InvoiceFields;
import com.tlc.crm.core.features.invoice.manager.InvoiceManager;
import com.tlc.crm.core.features.invoice.manager.InvoiceManagerImpl;
import com.tlc.crm.core.features.invoice.model.Invoice;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.web.HttpMethod;
import com.tlc.web.WebAction;

/**
 * <p>
 * Handles public access to invoice preview files.
 * </p>
 *
 * @author Mohamed Yasar k
 * @version 1.0
 */
@WebAction(path = "/invoice/{action}/{id}", method = HttpMethod.GET)
public class InvoiceRetrievalManagement extends AbstractSalesDocumentRetrievalManagement {

    private final InvoiceManager invoiceManager;

    public InvoiceRetrievalManagement() {
        this.invoiceManager = InvoiceManagerImpl.getInstance();
    }

    /**
     * <p>
     * Processes the invoice preview request and redirects the client to the
     * generated attachment preview URL.
     * </p>
     *
     * @param orgId      Organization identifier.
     * @param documentId Invoice identifier.
     * @param actionType Requested action type.
     * @param response   Response object used to redirect the client to the preview URL.
     */
    @Override
    protected void processDocument(final long orgId, final long documentId, final String actionType, final CrmResponse response) {
        final Invoice invoice = invoiceManager.get(orgId, documentId);

        if (invoice != null && invoice.getStatus() != Invoice.Status.DRAFT) {
            final boolean isDownloadAction = actionType.equalsIgnoreCase(InvoiceFields.DOWNLOAD);
            final Long attachmentId = isDownloadAction ? invoice.getPdfFileId() : invoice.getHtmlFileId();

            handleRedirection(attachmentId, isDownloadAction, response);
        }
    }

    /**
     * <p>
     * Returns the invoice identifier request parameter.
     * </p>
     *
     * @return invoice identifier parameter.
     */
    @Override
    protected String getIdParameter() {
        return InvoiceFields.ID;
    }

    /**
     * <p>
     * Returns the invoice action request parameter.
     * </p>
     *
     * @return invoice action parameter.
     */
    @Override
    protected String getActionParameter() {
        return InvoiceFields.ACTION;
    }
}