package com.tlc.crm.core.features.internal.dataimport.holder;

import com.tlc.attachment.model.Record;
import com.tlc.crm.core.features.internal.dataimport.holder.impl.ImportableRecordHolder;

/**
 * Container holds the column data of a row in an Excel file
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public final class ImportableRecord extends Record {

    private final ImportableRecordHolder importableRecordHolder;

    public ImportableRecord(final int rowIndex, final int size, final ImportableRecordHolder importableRecordHolder) {
        super(rowIndex, size);
        this.importableRecordHolder = importableRecordHolder;
    }

    /**
     * {@inheritDoc}
     *
     * @param columnIndex Represents the index of the column
     * @param value       Which contains the data to be added
     */
    @Override
    public void add(final int columnIndex, final Object value) {
        super.add(columnIndex, value);
        importableRecordHolder.add(columnIndex, index(), value);
    }
}