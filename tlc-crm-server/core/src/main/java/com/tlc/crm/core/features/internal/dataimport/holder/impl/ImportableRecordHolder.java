package com.tlc.crm.core.features.internal.dataimport.holder.impl;

import com.tlc.attachment.model.DefaultRecordHolder;
import com.tlc.attachment.model.Record;
import com.tlc.crm.core.features.internal.dataimport.holder.ImportableRecord;
import com.tlc.crm.entity.models.FieldInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * <p>
 * Record Holder for handling importable records that inherits the behaviour of {@link DefaultRecordHolder}
 * </p>
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public final class ImportableRecordHolder extends DefaultRecordHolder {

    private final Map<Integer, FieldInfo> mergeableFieldIndicesMap;
    private final Map<FieldInfo, Map<Object, Collection<Integer>>> indexedData;

    public ImportableRecordHolder(final Map<Integer, FieldInfo> mergeableFieldIndicesMap) {
        this.mergeableFieldIndicesMap = mergeableFieldIndicesMap;
        this.indexedData = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     *
     * @param index      Represents the index of the column
     * @param columnSize Represents the size of the headers
     * @return The instance of {@link Record}
     */
    @Override
    public Record createRecord(final int index, final int columnSize) {
        return new ImportableRecord(index, columnSize, this);
    }

    /**
     * <p>
     * Adds the data to the field value indices map
     * </p>
     *
     * @param columnIndex Represents the column index
     * @param rowIndex    Represents the row index
     * @param value       Represents the value
     */
    public void add(final int columnIndex, final int rowIndex, final Object value) {
        if (mergeableFieldIndicesMap.containsKey(columnIndex)) {
            final FieldInfo fieldInfo = mergeableFieldIndicesMap.get(columnIndex);
            final Map<Object, Collection<Integer>> indexedMap = indexedData.computeIfAbsent(fieldInfo,
                    key -> new HashMap<>());

            if (fieldInfo.storeType().isArrayType() && value instanceof final String stringValues) {
                final String[] values = stringValues.split(",");

                for (final String stringValue : values) {
                    indexedMap.computeIfAbsent(stringValue.trim(), key -> new TreeSet<>()).add(rowIndex);
                }
            } else {
                indexedMap.computeIfAbsent(value, key -> new TreeSet<>()).add(rowIndex);
            }
        }
    }

    /**
     * <p>
     * Gets the field value indices map
     * </p>
     *
     * @return The map contains field info as key and value and its indices map as values
     */
    public Map<FieldInfo, Map<Object, Collection<Integer>>> getIndexedData() {
        return indexedData;
    }
}