package com.tlc.attachment.generator.internal;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import com.tlc.attachment.storage.status.StorageErrorCodes;
import com.tlc.commons.code.ErrorCode;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;

/**
 * Generates PDF documents from HTML content using Playwright Chromium rendering engine.
 *
 * <p>This generator converts HTML pages into PDF format by loading the provided HTML content
 * into a headless Chromium browser page and exporting the rendered page as a PDF document.</p>
 *
 * <p>A singleton instance is used to reuse the Playwright browser lifecycle across multiple
 * PDF generation requests. A write lock is applied during rendering to ensure that PDF generation
 * operations are executed safely and avoid concurrent browser rendering conflicts.</p>
 *
 * <p>The generated PDF preserves browser-based rendering behavior including CSS styles,
 * images, fonts, and print background properties.</p>
 *
 * @author Sathish
 * @version 1.0
 * @see PlaywrightService
 * @see AbstractHtmlContentFileGenerator
 */
public final class PdfHtmlContentFileGenerator extends AbstractHtmlContentFileGenerator {

    private static final String PAGE_FORMAT = "A4";
    private static final double DEFAULT_TIMEOUT = 30_000;
    private static final double PDF_TIMEOUT = 60_000;

    private final Semaphore pdfLimiter;

    private PdfHtmlContentFileGenerator() {
        pdfLimiter = new Semaphore(10); // Limit concurrent PDF generation to 10
    }

    private static class Instance {
        private static final PdfHtmlContentFileGenerator INSTANCE = new PdfHtmlContentFileGenerator();
    }

    /**
     * Gets the singleton instance of {@link PdfHtmlContentFileGenerator}.
     *
     * @return {@link PdfHtmlContentFileGenerator} instance.
     */
    public static PdfHtmlContentFileGenerator getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts the provided HTML content into a PDF document and writes it to the given output stream.
     *
     * <p>The method performs the following steps:</p>
     * <ul>
     *     <li>Acquires a rendering lock to prevent concurrent PDF generation conflicts.</li>
     *     <li>Creates a new Chromium page from the shared browser instance.</li>
     *     <li>Loads the HTML content and waits until network resources are fully loaded.</li>
     *     <li>Generates an A4 PDF with background styles enabled.</li>
     *     <li>Writes the generated PDF bytes to the provided output stream.</li>
     * </ul>
     *
     * @param htmlContent  HTML content to render as PDF.
     * @param outputStream destination stream where generated PDF bytes are written.
     *
     * @throws IOException if writing PDF content to the output stream fails.
     */
    @Override
    protected void write(final String htmlContent, final OutputStream outputStream) throws IOException {
        final Browser browser = PlaywrightService.getInstance().browser();

        try {
            pdfLimiter.acquire();

            try (final BrowserContext context = browser.newContext(); final Page page = context.newPage()) {

                page.setDefaultTimeout(DEFAULT_TIMEOUT);
                page.setDefaultNavigationTimeout(DEFAULT_TIMEOUT);
                page.setContent(htmlContent, new Page.SetContentOptions().setTimeout(PDF_TIMEOUT).setWaitUntil(WaitUntilState.NETWORKIDLE));
                final byte[] pdfBytes = page.pdf(new Page.PdfOptions().setFormat(PAGE_FORMAT).setPrintBackground(true));

                outputStream.write(pdfBytes);
            } finally {
                pdfLimiter.release();
            }
        } catch (final Exception exception) {
            throw ErrorCode.get(StorageErrorCodes.PDF_GENERATION_FAILED, "Failed to generate PDF from HTML content", exception);
        }
    }
}