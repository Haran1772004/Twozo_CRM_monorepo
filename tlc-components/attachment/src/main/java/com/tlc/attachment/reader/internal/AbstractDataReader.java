package com.tlc.attachment.reader.internal;

import com.tlc.attachment.io.File;
import com.tlc.attachment.reader.DataReader;
import com.tlc.attachment.model.RecordHolder;

import java.util.Collection;
import java.util.Objects;

/**
 * Provides an implementation for {@link DataReader} offers a generalized approach to read,
 * without diving into specific details or concrete implementations.
 *
 * @author sundar
 * @version 1.0
 * @see DataReader
 */
abstract class AbstractDataReader implements DataReader {

    /**
     * {@inheritDoc}
     *
     * @param file Represents the Excel file contains the data to read
     * @return {@link RecordHolder} that contains the collection parsed Excel file rows
     */
    @Override
    public RecordHolder read(final File file) {
        final RecordHolder recordHolder = RecordHolder.create();

        read(file, 1, MAX_ROW_LENGTH, recordHolder);

        return recordHolder;
    }

    /**
     * {@inheritDoc}
     *
     * @param file Represents the Excel file contains the data to read
     */
    @Override
    public void read(final File file, final RecordHolder recordHolder) {
        read(file, 1, MAX_ROW_LENGTH, recordHolder);
    }

    /**
     * {@inheritDoc}
     *
     * @param file  Represents the Excel file contains the data to read
     * @param start Indicates the index (1-based) from which the Excel file should be read, excluding the header row
     * @param limit Refers to where the reading of the file should be stopped
     */
    @Override
    public void read(final File file, final int start, final int limit, final RecordHolder recordHolder) {
        Objects.requireNonNull(file);
        process(file, recordHolder, null, start, limit);
    }

    /**
     * {@inheritDoc}
     *
     * @param file         Represents the Excel file contains the data to read
     * @param indices      The indices to include in the output RecordHolder object
     * @param recordHolder The {@link RecordHolder} object to populate with the read data
     */
    @Override
    public void read(final File file, final Collection<Integer> indices, final RecordHolder recordHolder) {
        Objects.requireNonNull(file);
        process(file, recordHolder, indices, null, null);
    }

    /**
     * Processes the reading records from provided Excel {@link File} and loads them into the provided {@link RecordHolder}
     *
     * @param file         Represents the Excel file contains the data to read
     * @param recordHolder Represents the {@link RecordHolder} will be loaded with the read data
     * @param indices      Which contains the specific indices to be read
     * @param start        Indicates the starting index from where the file reading should begin
     * @param limit        Indicates the number of records to be read from the file
     */
    abstract void process(final File file, final RecordHolder recordHolder, final Collection<Integer> indices,
                          final Integer start, final Integer limit);
}
