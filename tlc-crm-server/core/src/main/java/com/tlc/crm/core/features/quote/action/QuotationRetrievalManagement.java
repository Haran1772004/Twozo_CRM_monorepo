package com.tlc.crm.core.features.quote.action;

import com.tlc.crm.core.features.sales.template.AbstractSalesDocumentRetrievalManagement;
import com.tlc.crm.core.features.quote.fields.QuotationFields;
import com.tlc.crm.core.features.quote.manager.QuotationManager;
import com.tlc.crm.core.features.quote.model.Quotation;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.web.HttpMethod;
import com.tlc.web.WebAction;

/**
 * <p>
 * This action validates the encoded quotation identifier, retrieves the
 * corresponding quotation details, verifies the generated attachment exists,
 * and redirects the request to a temporary pre-signed URL for preview access.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@WebAction(path = "/quotation/{action}/{id}", method = HttpMethod.GET)
public class QuotationRetrievalManagement extends AbstractSalesDocumentRetrievalManagement {

    private final QuotationManager quotationManager;

    public QuotationRetrievalManagement() {
        this.quotationManager = QuotationManager.getInstance();
    }

    /**
     * <p>
     * Processes the quotation preview request and redirects the client to the
     * generated attachment preview URL.
     * </p>
     *
     * @param orgId      Organization identifier.
     * @param documentId Quotation identifier.
     * @param actionType Requested action type.
     * @param response   Response object used to redirect the client to the preview URL.
     */
    @Override
    protected void processDocument(final long orgId, final long documentId, final String actionType, final CrmResponse response) {
        final Quotation quotation = quotationManager.get(orgId, documentId);

        if (quotation != null && quotation.getStatus() != Quotation.Status.DRAFT) {
            final boolean isDownloadAction = actionType.equalsIgnoreCase(QuotationFields.DOWNLOAD);
            final Long attachmentId = isDownloadAction ? quotation.getPdfFileId() : quotation.getHtmlFileId();

            handleRedirection(attachmentId, isDownloadAction, response);
        }
    }

    /**
     * <p>
     * Returns the quotation identifier request parameter.
     * </p>
     *
     * @return quotation identifier parameter.
     */
    @Override
    protected String getIdParameter() {
        return QuotationFields.ID;
    }

    /**
     * <p>
     * Returns the quotation action request parameter.
     * </p>
     *
     * @return quotation action parameter.
     */
    @Override
    protected String getActionParameter() {
        return QuotationFields.ACTION;
    }
}