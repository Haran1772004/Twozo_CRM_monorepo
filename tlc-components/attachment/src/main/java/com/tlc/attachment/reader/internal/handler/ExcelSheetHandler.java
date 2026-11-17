package com.tlc.attachment.reader.internal.handler;

import com.tlc.attachment.internal.status.AttachmentErrorCodes;
import com.tlc.attachment.model.RecordHolder;
import com.tlc.attachment.model.Record;
import com.tlc.commons.code.ErrorCode;

import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handler responsible for processing the rows and column values of an Excel sheet and then loading this data into the {@link RecordHolder}
 *
 * @author sundar
 * @version 1.0
 */
public class ExcelSheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {

    private static final byte HEADER_INDEX = 0;
    private static final byte DEFAULT_START = 1;
    private static final byte DEFAULT_LIMIT = 20;
    private static final Pattern COLUMN_REFERENCE_PATTERN = Pattern.compile("[A-Za-z]+");

    private final List<String> headerCellReferences;
    private final RecordHolder recordHolder;
    private final Predicate<Integer> headerCriteria;
    private final Predicate<Integer> specificIndexCriteria;
    private final boolean hasSpecificIndices;

    private Collection<Integer> specificIndices;
    private Record record;

    private boolean headersNotAdded;
    private int headerSize;
    private int startIndex;
    private int endIndex;
    private int currentIndex;

    private ExcelSheetHandler(final RecordHolder recordHolder, final boolean hasSpecificIndices) {
        this.recordHolder = recordHolder;
        this.hasSpecificIndices = hasSpecificIndices;
        this.headersNotAdded = true;
        this.headerCellReferences = new ArrayList<>(recordHolder.headers());
        this.headerCriteria = (index) -> (headersNotAdded && HEADER_INDEX == index);
        this.specificIndexCriteria = (index) -> hasSpecificIndices && !specificIndices.contains(index);
    }

    public ExcelSheetHandler(final RecordHolder recordHolder, final int start, final int limit) {
        this(recordHolder, false);
        this.startIndex = Math.max(start, 1);
        this.endIndex = startIndex > 1 ? (startIndex + (limit - 1)) : limit;
    }

    public ExcelSheetHandler(final RecordHolder recordHolder, final Collection<Integer> specificIndices) {
        this(recordHolder, Objects.nonNull(specificIndices));
        this.startIndex = DEFAULT_START;
        this.endIndex = DEFAULT_LIMIT;

        if (hasSpecificIndices) {
            final List<Integer> sortedSpecificIndices = specificIndices.stream().sorted().toList();

            if (!sortedSpecificIndices.isEmpty()) {
                this.startIndex = sortedSpecificIndices.get(0);
                this.endIndex = startIndex + sortedSpecificIndices.get(sortedSpecificIndices.size() - 1);
            }
            this.specificIndices = specificIndices;
        }
    }

    /**
     * Performs certain activities when a row is ready to be read
     *
     * @param index Represents the index
     */
    @Override
    public void startRow(final int index) {
        if (endIndex < index) {
            recordHolder.setHasMore(true);
            throw ErrorCode.get(AttachmentErrorCodes.MAX_READ_LIMIT_REACHED);
        }

        if (headerCriteria.or(specificIndexCriteria).test(index)) {
            return;
        }

        if (startIndex <= index) {
            this.currentIndex = index;
            this.record = recordHolder.createRecord(index, headerSize);
        }
    }

    /**
     * Performs certain activities when a row has been fully read
     *
     * @param index Represents the index
     */
    @Override
    public void endRow(final int index) {
        if (headerCriteria.test(index)) {
            this.headerSize = headerCellReferences.size();
            this.headersNotAdded = false;
        }

        if (Objects.nonNull(record) && !record.isEmpty()) {
            recordHolder.addRecord(index, record);
        }
        this.record = null;
    }

    /**
     * Handles the processing of activities when reading the cell value of a row in an Excel sheet
     *
     * @param cellReference  The reference of the cell being processed
     * @param formattedValue The value to be loaded into the corresponding {@link Record}
     * @param xssfComment    (Optional) Additional comment or explanation related to the XSSF (XML Spreadsheet Format) data
     */
    @Override
    public void cell(final String cellReference, final String formattedValue, final XSSFComment xssfComment) {

        if (headerCriteria.test(currentIndex)) {
            headerCellReferences.add(getColumnReference(cellReference));
            recordHolder.addHeader(formattedValue);
        }

        if (startIndex > currentIndex || specificIndexCriteria.test(currentIndex)) {
            return;
        }
        final int cellIndex = headerCellReferences.lastIndexOf(getColumnReference(cellReference));

        if (cellIndex > -1 && Objects.nonNull(record)) {
            record.add(cellIndex, formattedValue);
        }
    }

    /**
     * Gets the column reference of a cellReference without the row index of an Excel sheet
     *
     * @param cellReference Represents the reference of the cell being processed
     * @return The column reference without the row index
     */
    private static String getColumnReference(final String cellReference) {
        final Matcher matcher = COLUMN_REFERENCE_PATTERN.matcher(cellReference);

        return matcher.find() ? matcher.group() : null;
    }
}
