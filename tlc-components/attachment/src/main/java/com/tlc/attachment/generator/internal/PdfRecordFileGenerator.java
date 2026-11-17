package com.tlc.attachment.generator.internal;

import com.tlc.attachment.model.Record;
import com.tlc.attachment.model.RecordHolder;
import com.tlc.attachment.storage.status.StorageErrorCodes;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.font.Font;
import com.tlc.commons.font.store.FontRegistry;
import com.tlc.commons.util.StringUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides the service to write the data to a PDF file.
 *
 * <p>Each page renders a full table with a styled header row and alternating data rows.
 * When data exceeds the available page height, a new page is started automatically
 * and the header row is repeated at the top of every page.
 *
 * @author Sathish
 * @version 1.0
 * @see AbstractRecordFileGenerator
 */
public final class PdfRecordFileGenerator extends AbstractRecordFileGenerator {

    private static final String EMPTY_STRING = "";
    private static final String ELLIPSIS = "...";

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final float PAGE_MARGIN = 40f;
    private static final float ROW_HEIGHT = 18f;
    private static final float HEADER_ROW_HEIGHT = 20f;
    private static final float FONT_SIZE = 9f;
    private static final float CELL_PADDING = 4f;
    private static final float BORDER_WIDTH = 0.5f;

    // ── Colors (RGB 0-1 range) ────────────────────────────────────────────────
    private static final PDColor HEADER_BG_COLOR = pdColor(59, 120, 158);
    private static final PDColor HEADER_TXT_COLOR = pdColor(255, 255, 255);
    private static final PDColor ALT_ROW_COLOR = pdColor(242, 247, 251);
    private static final PDColor BORDER_COLOR = pdColor(180, 200, 215);
    private static final PDColor DATA_TXT_COLOR = pdColor(0, 0, 0);

    private static final String FONT_FAMILY = "Noto Sans";
    private static final String HEADER_FONT_NAME = "NotoSans-Bold";
    private static final String DATA_FONT_NAME = "NotoSans-Regular";

    private final FontRegistry fontRegistry;

    private PdfRecordFileGenerator() {
        fontRegistry = FontRegistry.getInstance();
    }

    private static class Instance {
        private static final PdfRecordFileGenerator INSTANCE = new PdfRecordFileGenerator();
    }

    /**
     * Gets the singleton instance of {@link PdfRecordFileGenerator}.
     *
     * @return {@link PdfRecordFileGenerator} instance.
     */
    public static PdfRecordFileGenerator getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param recordHolder Which contains the data to be written into the output stream
     * @param outputStream The stream to write PDF bytes into; must not be closed by this method
     * @throws IOException if there is an I/O error during the data writing process
     */
    @Override
    protected void writeData(final RecordHolder recordHolder, final OutputStream outputStream) throws IOException {
        final List<String> headers = recordHolder.headers();

        try (final PDDocument document = new PDDocument()) {
            final Collection<Font> fonts = fontRegistry.getFonts(FONT_FAMILY);
            final PDType0Font headerFont = getFont(HEADER_FONT_NAME, fonts, document);
            final PDType0Font dataFont = getFont(DATA_FONT_NAME, fonts, document);

            writeTable(document, headers, recordHolder.getRecords().values(), headerFont, dataFont);
            document.save(outputStream);
        }
    }

    /**
     * Renders all rows into a paginated table inside {@code document}.
     * A new page is added automatically when the current page runs out of space,
     * and the header row is repeated at the top of each new page.
     *
     * @param document The {@link PDDocument} to write into.
     * @param headers  Column header labels.
     * @throws IOException if an I/O error occurs while rendering.
     */
    private void writeTable(final PDDocument document, final List<String> headers, final Collection<Record> records,
                            final PDType0Font headerFont, final PDType0Font dataFont) throws IOException {
        final int totalColumns = headers.size();

        if (totalColumns == 0) {
            document.addPage(new PDPage(PDRectangle.A4));
            return;
        }
        final PDRectangle pageSize = resolvePageSize(totalColumns);

        final float pageWidth = pageSize.getWidth();
        final float pageHeight = pageSize.getHeight();
        final float tableWidth = pageWidth - 2 * PAGE_MARGIN;
        final float columnWidth = tableWidth / totalColumns;
        final float startX = PAGE_MARGIN;

        PDPage page = new PDPage(pageSize);
        document.addPage(page);

        PDPageContentStream contentStream = new PDPageContentStream(document, page);

        float currentY = pageHeight - PAGE_MARGIN;

        currentY = writeHeaderRow(contentStream, headers, headerFont, startX, currentY, columnWidth, totalColumns);

        int rowIndex = 0;

        try {
            for (final Record record : records) {

                if (currentY - ROW_HEIGHT < PAGE_MARGIN) {
                    contentStream.close();

                    page = new PDPage(pageSize);
                    document.addPage(page);

                    contentStream = new PDPageContentStream(document, page);

                    currentY = pageHeight - PAGE_MARGIN;
                    currentY = writeHeaderRow(contentStream, headers, headerFont, startX, currentY, columnWidth, totalColumns);
                }
                final List<String> values = record.getColumnValues().stream().map(this::stringify).toList();

                currentY = writeDataRow(contentStream, values, dataFont, startX, currentY, columnWidth, totalColumns, rowIndex % 2 != 0);

                rowIndex++;
            }

        } finally {
            contentStream.close();
        }
    }

    /**
     * Renders the header row with a coloured background and white bold text.
     *
     * @param contentStream The active {@link PDPageContentStream} to draw into.
     * @param headers       Column header labels; one label per column.
     * @param font          Bold font used for header text.
     * @param startX        Left edge X-coordinate of the table (equal to the page margin).
     * @param startY        Y-coordinate of the top edge of the header row.
     * @param columnWidth   Width in points of each column.
     * @param totalColumns  Total number of columns in the table.
     * @return The Y-coordinate immediately below the header row (ready for the first data row).
     * @throws IOException if a PDFBox I/O error occurs while drawing.
     */
    private float writeHeaderRow(final PDPageContentStream contentStream, final List<String> headers,
                                 final PDType0Font font, final float startX, final float startY,
                                 final float columnWidth, final int totalColumns) throws IOException {
        final float rowY = startY - HEADER_ROW_HEIGHT;

        contentStream.setNonStrokingColor(HEADER_BG_COLOR);
        contentStream.addRect(startX, rowY, columnWidth * totalColumns, HEADER_ROW_HEIGHT);
        contentStream.fill();

        contentStream.setNonStrokingColor(HEADER_TXT_COLOR);

        if (Objects.nonNull(font)) {
            contentStream.setFont(font, FONT_SIZE);
        }

        for (int colIndex = 0; colIndex < totalColumns; colIndex++) {
            final String text = truncateText(headers.get(colIndex), font, columnWidth - 2 * CELL_PADDING);

            contentStream.beginText();
            contentStream.newLineAtOffset(buildTX(startX, colIndex, columnWidth), rowY + CELL_PADDING);
            contentStream.showText(text);
            contentStream.endText();
        }

        drawRowBorders(contentStream, startX, rowY, columnWidth, HEADER_ROW_HEIGHT, totalColumns);
        return rowY;
    }

    /**
     * Retrieves and loads a PDF font from the provided font collection.
     *
     * <p>The method searches for a font definition by name, loads the corresponding
     * font file into the given {@link PDDocument} as a {@link PDType0Font}, and
     * returns the loaded font instance for PDF rendering.
     *
     * <p>If no matching font is found, this method returns {@code null}. If the font
     * file cannot be loaded, an appropriate storage error is thrown.
     *
     * @param fontName Name of the font to load.
     * @param fonts    Collection of available font definitions.
     * @param document PDF document where the font will be embedded.
     * @return Loaded {@link PDType0Font} instance, or {@code null} if the font is not found.
     * @throws ErrorCode if the font file cannot be loaded into the PDF document.
     */
    private PDType0Font getFont(final String fontName, final Collection<Font> fonts, final PDDocument document) {
        final Optional<Font> fontOptional = fonts.stream()
                .filter(font -> font.name().equalsIgnoreCase(fontName))
                .findFirst();

        return fontOptional.map(font -> {
            try {
                return PDType0Font.load(document, font.file());
            } catch (IOException e) {
                throw ErrorCode.get(StorageErrorCodes.FILE_WRITE_ACTION_FAILED,
                        StringUtil.join(StringUtil.Delimiter.EMPTY_STRING, "Failed to load font: ", fontName, e.getMessage()));
            }
        }).orElse(null);
    }

    /**
     * Renders a single data row, applying an alternate background colour for odd-indexed rows.
     *
     * @param contentStream  The active {@link PDPageContentStream} to draw into.
     * @param values         Cell values for this row; one entry per column.
     * @param font           Regular font used for data text.
     * @param startX         Left edge X-coordinate of the table.
     * @param startY         Y-coordinate of the top edge of this row.
     * @param columnWidth    Width in points of each column.
     * @param totalColumns   Total number of columns in the table.
     * @param isAlternateRow {@code true} if the row should receive the alternate
     *                       background colour ({@link #ALT_ROW_COLOR}).
     * @return The Y-coordinate immediately below the rendered row.
     * @throws IOException if a PDFBox I/O error occurs while drawing.
     */
    private float writeDataRow(final PDPageContentStream contentStream, final List<String> values, final PDType0Font font,
                               final float startX, final float startY, final float columnWidth, final int totalColumns,
                               final boolean isAlternateRow) throws IOException {
        final float rowY = startY - ROW_HEIGHT;

        if (isAlternateRow) {
            contentStream.setNonStrokingColor(ALT_ROW_COLOR);
            contentStream.addRect(startX, rowY, columnWidth * totalColumns, ROW_HEIGHT);
            contentStream.fill();
        }

        contentStream.setNonStrokingColor(DATA_TXT_COLOR);
        contentStream.setFont(font, FONT_SIZE);

        for (int colIndex = 0; colIndex < totalColumns; colIndex++) {
            final String value = colIndex < values.size() ? values.get(colIndex) : EMPTY_STRING;
            final String text = truncateText(value, font, columnWidth - 2 * CELL_PADDING);

            contentStream.beginText();
            contentStream.newLineAtOffset(buildTX(startX, colIndex, columnWidth), rowY + CELL_PADDING);
            contentStream.showText(text);
            contentStream.endText();
        }

        drawRowBorders(contentStream, startX, rowY, columnWidth, ROW_HEIGHT, totalColumns);
        return rowY;
    }

    /**
     * Calculates the X-coordinate position for a cell value within a table column.
     *
     * <p>The position is calculated based on the table start position, the column
     * index, the width of each column, and the configured cell padding to align
     * the text inside the cell.
     *
     * @param startX      Starting X-coordinate of the table.
     * @param colIndex    Zero-based index of the column.
     * @param columnWidth Width of each table column.
     * @return The X-coordinate where the cell content should be rendered.
     */
    private float buildTX(final float startX, final int colIndex, final float columnWidth) {
        return startX + colIndex * columnWidth + CELL_PADDING;
    }

    /**
     * Draws the outer rectangle border and the inner vertical column dividers
     * for one row using {@link #BORDER_COLOR} and {@link #BORDER_WIDTH}.
     *
     * @param contentStream The active {@link PDPageContentStream} to draw into.
     * @param startX        Left edge X-coordinate of the row / table.
     * @param rowY          Bottom edge Y-coordinate of the row.
     * @param columnWidth   Width in points of each column.
     * @param rowHeight     Height in points of the row.
     * @param totalColumns  Total number of columns; determines how many dividers
     *                      are drawn.
     * @throws IOException if a PDFBox I/O error occurs while drawing.
     */
    private void drawRowBorders(final PDPageContentStream contentStream, final float startX,
                                final float rowY, final float columnWidth, final float rowHeight,
                                final int totalColumns) throws IOException {
        contentStream.setStrokingColor(BORDER_COLOR);
        contentStream.setLineWidth(BORDER_WIDTH);

        contentStream.addRect(startX, rowY, columnWidth * totalColumns, rowHeight);
        contentStream.stroke();

        for (int colIndex = 1; colIndex < totalColumns; colIndex++) {
            final float dividerX = startX + colIndex * columnWidth;

            contentStream.moveTo(dividerX, rowY);
            contentStream.lineTo(dividerX, rowY + rowHeight);
            contentStream.stroke();
        }
    }

    /**
     * Resolves the page size based on the number of columns.
     * Wide tables (more than 5 columns) use A4 landscape; narrower tables use A4 portrait.
     *
     * @param totalColumns Number of columns in the table.
     * @return The appropriate {@link PDRectangle} page size.
     */
    private PDRectangle resolvePageSize(final int totalColumns) {
        return totalColumns > 5
                ? new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth())
                : PDRectangle.A4;
    }

    /**
     * Truncates {@code text} so that its rendered width fits within {@code maxWidth} points,
     * appending an ellipsis ("…") when the text is shortened.
     * Characters outside the WinAnsiEncoding range are removed before measuring.
     *
     * @param text     The text to evaluate.
     * @param font     The font used for width calculation.
     * @param maxWidth Maximum allowed width in points.
     * @return The (possibly truncated) text that fits within {@code maxWidth}.
     */
    private String truncateText(final String text, final PDType0Font font, final float maxWidth) {
        final String normalized = normalize(text);

        if (normalized.isEmpty()) {
            return EMPTY_STRING;
        }
        final String normalizedText = normalized.length() > 10 ? normalized.substring(0, 10) : normalized;

        if (Objects.isNull(font)) {
            return normalizedText;
        }

        try {
            final float textWidth = font.getStringWidth(normalized) / 1000f * FONT_SIZE;

            if (textWidth <= maxWidth) {
                return normalized;
            }
            final float ellipsisWidth = font.getStringWidth(ELLIPSIS) / 1000f * FONT_SIZE;

            int low = 0;
            int high = normalized.length();

            while (low < high) {
                final int mid = (low + high + 1) / 2;
                final String candidate = normalized.substring(0, mid);
                final float width = font.getStringWidth(candidate) / 1000f * FONT_SIZE + ellipsisWidth;

                if (width <= maxWidth) {
                    low = mid;
                } else {
                    high = mid - 1;
                }
            }

            return low > 0 ? normalized.substring(0, low) + ELLIPSIS : EMPTY_STRING;
        } catch (IOException e) {
            return normalizedText;
        }
    }

    /**
     * Strips characters that cannot be encoded in WinAnsiEncoding (the encoding used by
     * the standard Type1 fonts). Characters outside the range 0x20–0xFF are removed.
     *
     * @param text Raw text value.
     * @return Sanitized text safe for embedding in a standard PDF Type1 font stream.
     */
    private String normalize(final String text) {
        return Objects.isNull(text) ? EMPTY_STRING : text;
    }

    /**
     * Converts a value to its string representation, returning an empty string for {@code null}.
     *
     * @param value Any object.
     * @return Non-null string representation.
     */
    private String stringify(final Object value) {
        return Optional.ofNullable(value).map(String::valueOf).orElse(EMPTY_STRING);
    }

    /**
     * Factory helper that creates a {@link PDColor} from standard 0–255 RGB integer components.
     *
     * @param red   Red component (0–255).
     * @param green Green component (0–255).
     * @param blue  Blue component (0–255).
     * @return A {@link PDColor} in the DeviceRGB colour space.
     */
    private static PDColor pdColor(final int red, final int green, final int blue) {
        return new PDColor(new float[]{red / 255f, green / 255f, blue / 255f}, PDDeviceRGB.INSTANCE);
    }
}


