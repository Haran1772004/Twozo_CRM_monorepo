package com.tlc.attachment.generator.internal;

import com.tlc.attachment.model.Record;
import com.tlc.attachment.model.RecordHolder;

import com.tlc.commons.Counter;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;


import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provides the service to write the data to SpreadsheetML worksheet.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ExcelRecordFileGenerator extends AbstractRecordFileGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelRecordFileGenerator.class);

    private static class Instance {
        private static final ExcelRecordFileGenerator INSTANCE = new ExcelRecordFileGenerator();
    }

    /**
     * Gets instance of {@link ExcelRecordFileGenerator}.
     *
     * @return {@link ExcelRecordFileGenerator} instance.
     */
    public static ExcelRecordFileGenerator getInstance() {
        return Instance.INSTANCE;
    }

    private ExcelRecordFileGenerator() {
    }

    /**
     * {@inheritDoc}
     *
     * @param recordHolder Which contains the data to be written into the output stream
     * @param outputStream The stream to write Excel bytes into; must not be closed by this method
     * @throws IOException if there is an I/O error during the data writing process
     */
    @Override
    protected void writeData(final RecordHolder recordHolder, final OutputStream outputStream) throws IOException {

        try (final Workbook workbook = new SXSSFWorkbook()) {
            final Sheet sheet = workbook.createSheet();
            final List<String> headerRowList = recordHolder.headers();
            final int totalHeaders = headerRowList.size();
            final Map<Integer, Record> recordMap = recordHolder.getRecords();
            final long writeStartedAt = System.currentTimeMillis();

            writeHeaders(headerRowList, totalHeaders, sheet);

            if (!recordMap.isEmpty()) {
                final Counter rowIndexCounter = new Counter(1);

                for (final Map.Entry<Integer, Record> recordEntry : recordMap.entrySet()) {
                    final List<Object> columnValues = recordEntry.getValue().getColumnValues();
                    final Row row = sheet.createRow(rowIndexCounter.getAndIncrement());

                    for (int columnIndex = 0; columnIndex < totalHeaders; columnIndex++) {
                        final Object columnValue = columnIndex < columnValues.size() ? columnValues.get(columnIndex) : null;

                        if (Objects.nonNull(columnValue)) {
                            row.createCell(columnIndex).setCellValue(String.valueOf(columnValue));
                        }
                    }
                }
            }
            workbook.write(outputStream);
            LOGGER.debug("Excel file write operation completed in {} milliseconds", System.currentTimeMillis() - writeStartedAt);
        }
    }

    /**
     * Writes the header row into the Excel sheet
     *
     * @param headerRowList Contains the {@link List<String>} of header row to written into the Sheet
     * @param totalHeaders  Specifies the number of headers present in the headerRowList
     * @param sheet         Represents the {@link Sheet} where the header row will be written
     */
    private void writeHeaders(final List<String> headerRowList, final int totalHeaders, final Sheet sheet) {

        if (totalHeaders > 0) {
            final Row headerRow = sheet.createRow(0);

            for (int index = 0; index < headerRowList.size(); index++) {
                headerRow.createCell(index).setCellValue(headerRowList.get(index));
            }
        }
    }
}
