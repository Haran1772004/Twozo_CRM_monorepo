package com.tlc.attachment.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Holder that stores the headers and records of a read Excel file
 *
 * @author pandiaraj
 * @version 1.0
 */
public interface RecordHolder extends DataHolder {

    /**
     * Creates a {@link RecordHolder}
     *
     * @return {@link RecordHolder}
     */
    static RecordHolder create() {
        return new DefaultRecordHolder();
    }

    /**
     * <p>
     * Creates and returns the instance of {@link Record}
     * </p>
     *
     * @param index      Represents the index of the column
     * @param columnSize Represents the size of the headers
     * @return The instance of {@link Record}
     */
    Record createRecord(final int index, final int columnSize);

    /**
     * Adds a collection of headers to the {@link RecordHolder} in constant time complexity (O(1)).
     *
     * @param header Refers the Excel sheet column header
     */
    void addHeader(final String header);

    /**
     * Adds a collection of headers to the {@link RecordHolder} with a time complexity of O(n), where n represents the number of headers in the collection
     *
     * @param headers Which contains the collection headers
     */
    void addHeaders(final Collection<String> headers);

    /**
     * <p>
     * Adds the record located at the specified index in the Excel sheet to the values collection.
     * The time complexity of adding a record to the record holder is O(log n).
     * If the provided index already exists, the previous value will be overwritten with the new value.
     * </p>
     *
     * @param index  Represents in which index the {@link Record} would be present. Starts from 1
     * @param record Which contains the data of an Excel sheet record
     */
    void addRecord(final int index, final Record record);

    /**
     * Gets the {@link Collection} of headers which contains the headers of the Excel file
     *
     * @return {@link Collection} of headers
     */
    List<String> headers();

    /**
     * Returns a {@link Map} containing records read from the Excel file, grouped by their indices
     *
     * @return A {@link Map} with records organized by index
     */
    Map<Integer, Record> getRecords();

    /**
     * Refers whether the read files has more records or not
     *
     * @return true if the file has more records to read otherwise false
     */
    boolean hasMore();

    /**
     * Retrieves the total number of records, including the header row, contained within this {@link RecordHolder}
     *
     * @return the total number of records
     */
    int totalRecords();

    /**
     * Sets the 'hasMore' flag to indicate whether there are additional records to be read in the Excel file
     *
     * @param hasMore A boolean flag that signifies the presence of more records to be read in the given file
     */
    void setHasMore(final boolean hasMore);
}
