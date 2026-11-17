package com.tlc.crm.core.features.quote.util;

import com.tlc.attachment.io.Extension;
import com.tlc.crm.core.features.sales.template.service.AbstractSalesDocumentURLGenerator;

/**
 * <p>
 * Utility class responsible for generating quotation URLs dynamically at fetch time.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class QuotationURLGenerator extends AbstractSalesDocumentURLGenerator {

    private static final String CONFIGURATION_NAME = "quotation";
    private static final String QUOTE_FILE = "quoteFile";

    private QuotationURLGenerator() {
        super(CONFIGURATION_NAME);
    }

    private static final class InstanceHolder {
        private static final QuotationURLGenerator QUOTATION_URL_GENERATOR = new QuotationURLGenerator();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link QuotationURLGenerator}.
     * </p>
     *
     * @return the singleton instance
     */
    public static QuotationURLGenerator getInstance() {
        return InstanceHolder.QUOTATION_URL_GENERATOR;
    }

    /**
     * <p>
     * Generates the absolute quotation preview URL for the given quotation ID.
     * </p>
     *
     * @param orgId       Represents the organization ID.
     * @param userId      Represents the user ID.
     * @param quotationId The primary-key ID of the quotation.
     * @return The absolute preview quotation URL.
     */
    public String generatePreviewURL(final long orgId, final long userId, final long quotationId) {
        return super.generatePreviewURL(orgId, userId, quotationId);
    }

    /**
     * <p>
     * Generates the absolute quotation download URL for the given quotation ID.
     * </p>
     *
     * @param orgId       Represents the organization ID.
     * @param userId      Represents the user ID.
     * @param quotationId The primary-key ID of the quotation.
     * @return The absolute quotation download URL.
     */
    public String generateDownloadURL(final long orgId, final long userId, final long quotationId) {
        return super.generateDownloadURL(orgId, userId, quotationId);
    }

    /**
     * <p>
     * Generates the file path for storing an uploaded quotation file based on the organization,
     * file identifier, and file extension.
     * </p>
     *
     * @param orgId     Organization identifier used to create the storage path.
     * @param id        File identifier used as the file name.
     * @param extension File extension of the generated file.
     * @return Absolute file path for storing the quotation file.
     */
    public String generateFilePath(final Long orgId, final Long id, final Extension extension) {
        return super.generateFilePath(orgId, id, QUOTE_FILE, extension);
    }
}