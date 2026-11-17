package com.tlc.crm.core.features.invoice.service;

import com.tlc.attachment.io.Extension;
import com.tlc.crm.core.features.sales.template.service.AbstractSalesDocumentURLGenerator;

/**
 * <p>
 * class responsible for generating invoice URLs dynamically at fetch time.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class InvoiceURLGenerator extends AbstractSalesDocumentURLGenerator {

    private static final String CONFIGURATION_NAME = "invoice";
    private static final String INVOICE_FILE = "invoiceFile";

    private InvoiceURLGenerator() {
        super(CONFIGURATION_NAME);
    }

    private static final class InstanceHolder {
        private static final InvoiceURLGenerator INVOICE_URL_GENERATOR = new InvoiceURLGenerator();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link InvoiceURLGenerator}.
     * </p>
     *
     * @return the singleton instance
     */
    public static InvoiceURLGenerator getInstance() {
        return InstanceHolder.INVOICE_URL_GENERATOR;
    }

    /**
     * <p>
     * Generates the absolute invoice preview URL for the given invoice ID.
     * </p>
     *
     * @param orgId     Represents the organization ID.
     * @param userId    Represents the user ID.
     * @param invoiceId The primary-key ID of the invoice.
     * @return The absolute preview invoice URL.
     */
    public String generatePreviewURL(final long orgId, final long userId, final long invoiceId) {
        return super.generatePreviewURL(orgId, userId, invoiceId);
    }

    /**
     * <p>
     * Generates the absolute invoice download URL for the given invoice ID.
     * </p>
     *
     * @param orgId     Represents the organization ID.
     * @param userId    Represents the user ID.
     * @param invoiceId The primary-key ID of the invoice.
     * @return The absolute invoice download URL.
     */
    public String generateDownloadURL(final long orgId, final long userId, final long invoiceId) {
        return super.generateDownloadURL(orgId, userId, invoiceId);
    }

    /**
     * <p>
     * Generates the file path for storing an uploaded invoice file based on the organization,
     * file identifier, and file extension.
     * <p>
     *
     * @param orgId     Organization identifier used to create the storage path.
     * @param id        File identifier used as the file name.
     * @param extension File extension of the generated file.
     * @return Absolute file path for storing the invoice file.
     */
    public String generateFilePath(final Long orgId, final Long id, final Extension extension) {
        return super.generateFilePath(orgId, id, INVOICE_FILE, extension);
    }
}