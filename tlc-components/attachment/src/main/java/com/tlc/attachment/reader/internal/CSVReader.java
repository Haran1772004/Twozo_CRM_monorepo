package com.tlc.attachment.reader.internal;

import com.opencsv.exceptions.CsvValidationException;

import com.tlc.attachment.io.File;
import com.tlc.attachment.reader.DataReader;
import com.tlc.attachment.model.RecordHolder;
import com.tlc.attachment.model.Record;
import com.tlc.attachment.storage.status.StorageErrorCodes;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Provides implementations for parsing CSV-formatted files and storing their contents into a {@link RecordHolder}
 *
 * @author pandiaraj
 * @version 1.0
 * @see AbstractDataReader
 */
public class CSVReader extends AbstractDataReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSVReader.class);

    private static class INSTANCE {
        private static final CSVReader INSTANCE = new CSVReader();
    }

    /**
     * Gets the {@link DataReader} instance
     *
     * @return {@link DataReader}
     */
    public static DataReader getInstance() {
        return INSTANCE.INSTANCE;
    }

    private CSVReader() {
    }

    /**
     * {@inheritDoc}
     *
     * @param file         Represents the Excel file contains the data to read
     * @param recordHolder Represents the {@link RecordHolder} will be loaded with the read data
     * @param indices      Which contains the specific indices to be read
     * @param start        Indicates the starting index from where the file reading should begin
     * @param limit        Indicates the number of records to be read from the file
     */
    @Override
    void process(final File file, final RecordHolder recordHolder, final Collection<Integer> indices, final Integer start,
                 final Integer limit) {

        try (final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(file.inputStream()));
             final com.opencsv.CSVReader reader = new com.opencsv.CSVReader(bufferedReader)) {
            final boolean hasSpecificIndices = Objects.nonNull(indices);
            final Collection<String> headers = recordHolder.headers();
            final String[] csvHeaders = reader.readNext();
            final int from;
            final int to;

            recordHolder.addHeaders(Arrays.stream(csvHeaders).toList());

            if (hasSpecificIndices) {
                final List<Integer> sortedList = indices.stream().sorted().toList();

                from = sortedList.get(0);
                to = sortedList.get(sortedList.size() - 1);
            } else {
                from = Math.max(start, 1);
                to = from > 1 ? (from + (limit - 1)) : limit;
            }
            final int headerSize = headers.size();
            final int columnStart = headerSize - csvHeaders.length;
            final long readStartedAt = System.currentTimeMillis();
            final BiConsumer<Integer, String[]> rowAdder = (index, row) -> {
                final Record record = buildRecord(row, headerSize, columnStart, recordHolder.createRecord(index, headerSize));

                if (!record.isEmpty()) {
                    recordHolder.addRecord(index, record);
                }
            };
            String[] row;

            reader.skip(from - 1);

            for (int index = from; index <= to && (row = reader.readNext()) != null; index++) {

                if (hasSpecificIndices) {

                    if (indices.contains(index)) {
                        rowAdder.accept(index, row);
                    }
                } else {
                    rowAdder.accept(index, row);
                }
            }
            recordHolder.setHasMore(Objects.nonNull(reader.readNext()));
            final String fileName = file.name();

            LOGGER.info("{} reading was completed within a time frame of {} milliseconds", fileName, (System.currentTimeMillis() - readStartedAt));
            LOGGER.info("{} records have been successfully read from {}", recordHolder.totalRecords(), fileName);
        } catch (IOException exception) {
            throw ErrorCode.get(ErrorCodeProvider.UNKNOWN, exception);
        } catch (CsvValidationException exception) {
            throw ErrorCode.get(StorageErrorCodes.INVALID_CSV_FILE, exception);
        }
    }

    /**
     * Builds a {@link Record} from a row of data and its corresponding headers
     *
     * @param row         The row of data to convert to a {@link Record} object
     * @param headerSize  Represents the header length of the file
     * @param columnStart Indicates that the addition should start from the specified value in the column
     * @param record      Represents the {@link Record}
     * @return a {@link Record} object representing the input data
     */
    private Record buildRecord(final String[] row, final int headerSize, final int columnStart, final Record record) {
        for (int columnIndex = columnStart; columnIndex < headerSize; columnIndex++) {
            final String value = row[columnIndex - columnStart];

            record.add(columnIndex, Objects.nonNull(value) && value.isBlank() ? null : value);
        }
        return record;
    }

}
