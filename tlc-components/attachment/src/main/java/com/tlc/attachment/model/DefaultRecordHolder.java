package com.tlc.attachment.model;

import com.tlc.attachment.internal.status.AttachmentErrorCodes;
import com.tlc.commons.code.ErrorCode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Provides the implementation for the {@link RecordHolder}
 *
 * @author pandiaraj
 * @version 1.0
 * @see RecordHolder
 */
public class DefaultRecordHolder implements RecordHolder {

    private static final int MAX_RECORD_LIMIT = 10000;

    protected final List<String> headers;
    protected final Map<Integer, Record> records;

    protected boolean hasMore;

    protected DefaultRecordHolder() {
        this.headers = new ArrayList<>();
        this.records = new TreeMap<>(Comparator.naturalOrder());
    }

    /**
     * {@inheritDoc}
     *
     * @param index      Represents the index of the column
     * @param columnSize Represents the size of the headers
     * @return The {@link Record}
     */
    public Record createRecord(final int index, final int columnSize) {
        return new Record(index, columnSize);
    }

    /**
     * {@inheritDoc}
     *
     * @param header Refers the Excel sheet column header
     */
    @Override
    public void addHeader(final String header) {
        this.headers.add(header);
    }

    /**
     * {@inheritDoc}
     *
     * @param headers Which contains the collection headers
     */
    @Override
    public void addHeaders(final Collection<String> headers) {
        this.headers.addAll(headers);
    }

    /**
     * {@inheritDoc}
     *
     * @param index  Represents in which index the {@link Record} would be present. Starts from 1
     * @param record Which contains the data of an Excel sheet record
     */
    @Override
    public void addRecord(final int index, final Record record) {
        if (records.size() >= MAX_RECORD_LIMIT) {
            throw ErrorCode.get(AttachmentErrorCodes.MAX_RECORD_LIMIT_REACHED);
        }
        this.records.put(index, record);
    }

    /**
     * {@inheritDoc}
     *
     * @return A {@link Map} with records organized by index
     */
    @Override
    public Map<Integer, Record> getRecords() {
        return this.records;
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the file has more records to read otherwise false
     */
    @Override
    public boolean hasMore() {
        return hasMore;
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the {@link RecordHolder} has no headers or records
     */
    @Override
    public boolean isEmpty() {
        return headers.isEmpty() && records.isEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @return the total number of records
     */
    @Override
    public int totalRecords() {
        return records.size() + (headers.isEmpty() ? 0 : 1);
    }

    /**
     * {@inheritDoc}
     *
     * @param hasMore A boolean flag that signifies the presence of more records to be read in the given file
     */
    @Override
    public void setHasMore(final boolean hasMore) {
        this.hasMore = hasMore;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Collection} which contains the Excel sheet headers
     */
    @Override
    public List<String> headers() {
        return this.headers;
    }
}