package com.tlc.attachment.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Container holds the column data of a row in an Excel file
 *
 * @author sundar
 * @version 1.0
 */
public class Record {

    private final int index;
    private final int size;
    private final Map<Integer, Object> columnData;

    public Record(final int rowIndex, final int size) {
        this.index = rowIndex;
        this.size = size;
        this.columnData = new HashMap<>(size);
    }

    /**
     * Adds the value into this {@link Record} at the provided index
     *
     * @param index Represents the index of the column
     * @param value Which contains the data to be added
     */
    public void add(final int index, final Object value) {
        columnData.put(index, value);
    }

    /**
     * Gets the value from this {@link Record}
     *
     * @param index Represents the index of the column
     * @return value presents in the index
     */
    public Object get(final int index) {
        return columnData.get(index);
    }

    /**
     * Gets all column values presents inside this {@link Record}
     *
     * @return {@link List} of values of this {@link Record}
     */
    public List<Object> getColumnValues() {
        final List<Object> values = new ArrayList<>(size);

        for (int columnIndex = 0; columnIndex < size; columnIndex++) {
            values.add(columnData.getOrDefault(columnIndex, null));
        }
        return Collections.unmodifiableList(values);
    }

    /**
     * Checks whether the given index contains value or not
     *
     * @param columnIndex Represents the column index
     * @return true if any value presents in the provided index
     */
    public boolean exists(final int columnIndex) {
        return columnData.containsKey(columnIndex);
    }

    /**
     * Checks whether the record has values or not
     *
     * @return true if the record has values; otherwise false
     */
    public boolean isEmpty() {
        return columnData.isEmpty() || columnData.values().stream().allMatch(Objects::isNull);
    }

    /**
     * Gets the index of the {@link Record}
     *
     * @return index of this {@link Record}
     */
    public int index() {
        return index;
    }
}
