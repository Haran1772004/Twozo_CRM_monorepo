package com.tlc.attachment.generator;

import com.tlc.attachment.generator.internal.HtmlContentFileGeneratorImpl;
import com.tlc.attachment.generator.internal.PdfHtmlContentFileGenerator;
import com.tlc.attachment.io.Extension;
import com.tlc.attachment.model.HtmlContentHolder;
import com.tlc.attachment.storage.status.StorageErrorCodes;

import com.tlc.commons.code.ErrorCode;

/**
 * Template-method base class for writers that convert an HTML string into a rendered output file.
 *
 * @author Sathish
 * @version 1.0
 * @see ContentFileGenerator
 * @see FileCompressor
 */
public interface HtmlContentFileGenerator extends ContentFileGenerator<HtmlContentHolder> {

    /**
     * Returns the singleton {@link HtmlContentFileGenerator} for the given {@link Extension}.
     *
     * @param extension Target output format.
     * @return The matching {@link HtmlContentFileGenerator} singleton.
     * @throws ErrorCode wrapping {@link StorageErrorCodes#UNSUPPORTED_FILE_EXTENSION} if the extension is not supported.
     */
    static HtmlContentFileGenerator get(final Extension extension) {
        return switch (extension) {
            case PDF -> PdfHtmlContentFileGenerator.getInstance();
            case HTML -> HtmlContentFileGeneratorImpl.getInstance();
            default -> throw ErrorCode.get(StorageErrorCodes.UNSUPPORTED_FILE_EXTENSION);
        };
    }
}