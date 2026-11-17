package com.tlc.attachment.generator.internal;

import com.opencsv.CSVWriter;
import com.tlc.attachment.model.Record;
import com.tlc.attachment.model.RecordHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Provides the service to write the data to CSV file.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class CsvRecordFileGenerator extends AbstractRecordFileGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvRecordFileGenerator.class);
    private static final String EMPTY_STRING = "";
    private static final char UTF_8_BOM = '\uFEFF';

    private static class Instance {
        private static final CsvRecordFileGenerator INSTANCE = new CsvRecordFileGenerator();
    }

    /**
     * Gets instance of {@link CsvRecordFileGenerator}.
     *
     * @return {@link CsvRecordFileGenerator} instance.
     */
    public static CsvRecordFileGenerator getInstance() {
        return Instance.INSTANCE;
    }

    private CsvRecordFileGenerator() {
    }

    /**
     * {@inheritDoc}
     *
     * @param recordHolder Which contains the data to be written into the output stream
     * @param outputStream The stream to write CSV bytes into; must not be closed by this method
     * @throws IOException if there is an I/O error during the data writing process
     */
    @Override
    protected void writeData(final RecordHolder recordHolder, final OutputStream outputStream) throws IOException {

        try (final CSVWriter csvWriter = createCSVWriterWithBom(outputStream)) {
            final List<String> headerRowList = recordHolder.headers();
            final int totalHeaders = headerRowList.size();
            final Map<Integer, Record> recordMap = recordHolder.getRecords();
            final long writeStartedAt = System.currentTimeMillis();

            writeHeaders(headerRowList, totalHeaders, csvWriter);

            for (final Map.Entry<Integer, Record> recordEntry : recordMap.entrySet()) {
                final List<Object> sourceColumnValues = recordEntry.getValue().getColumnValues();
                final String[] targetColumnValues = new String[totalHeaders];

                for (int columnIndex = 0; columnIndex < totalHeaders; columnIndex++) {
                    targetColumnValues[columnIndex] = columnIndex < sourceColumnValues.size()
                            ? Optional.ofNullable(sourceColumnValues.get(columnIndex))
                            .map(String::valueOf).orElse(EMPTY_STRING) : EMPTY_STRING;
                }
                csvWriter.writeNext(targetColumnValues);
            }
            LOGGER.debug("CSV file write operation completed in {} milliseconds", System.currentTimeMillis() - writeStartedAt);
        }
    }

    /**
     * Creates a {@link CSVWriter} wrapping the given {@link OutputStream} and writes the UTF-8 BOM character.
     *
     * @param outputStream The stream to write into
     * @throws IOException if there is an I/O error while writing the BOM
     */
    private CSVWriter createCSVWriterWithBom(final OutputStream outputStream) throws IOException {
        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

        writer.write(UTF_8_BOM);

        return new CSVWriter(writer);
    }

    /**
     * Writes the headers into the file using {@link CSVWriter}
     *
     * @param headerRowList Contains the {@link List<String>} of header row
     * @param totalHeaders  Specifies the number of headers present in the headerRowList
     * @param csvWriter     Refers to the {@link CSVWriter} instance utilized for writing the headers into the file
     */
    private void writeHeaders(final List<String> headerRowList, final int totalHeaders, final CSVWriter csvWriter) {

        if (totalHeaders > 0) {
            final String[] headers = new String[totalHeaders];

            for (int index = 0; index < totalHeaders; index++) {
                headers[index] = headerRowList.get(index);
            }
            csvWriter.writeNext(headers);
        }
    }
}
