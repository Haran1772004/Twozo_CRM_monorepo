package com.tlc.crm.entity.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.option.OptionKeys;

/**
 * <p>
 *     A container which holds the options of a dropdown field
 * </p>
 *
 * @author Selvakumar
 */
public class OptionContainer {

    private final JsonArray options;
    private final String keyName;
    private final String displayKeyName;
    private final String order;
    private final String isEditableOption;
    private long totalRecords;

    public OptionContainer() {
        this(OptionKeys.VALUE, OptionKeys.NAME, OptionKeys.ORDER, OptionKeys.IS_EDITABLE_OPTION, 0);
    }

    public OptionContainer(final long totalRecords) {
        this(OptionKeys.VALUE, OptionKeys.NAME, OptionKeys.ORDER, OptionKeys.IS_EDITABLE_OPTION, totalRecords);
    }

    public OptionContainer(final String keyName, final String displayKeyName) {
        this(keyName, displayKeyName, 0);
    }

    public OptionContainer(final String keyName, final String displayKeyName, final long totalRecords) {
        this.options = Json.array();
        this.keyName = keyName;
        this.displayKeyName = displayKeyName;
        this.order = null;
        this.isEditableOption = null;
        this.totalRecords = totalRecords;
    }

    public OptionContainer(final String keyName, final String displayKeyName, final String order, final String isEditableOption,
                           final long totalRecords) {
        this.options = Json.array();
        this.keyName = keyName;
        this.displayKeyName = displayKeyName;
        this.order = order;
        this.isEditableOption = isEditableOption;
        this.totalRecords = totalRecords;
    }

    /**
     * <p>
     *     Adds the option details into container
     * </p>
     * @param value
     * @param displayValue
     */
    public void addOption(final Object value, final Object displayValue) {
        final JsonObject option = Json.object();

        option.put(displayKeyName, displayValue);
        option.put(keyName, value);

        addOption(option);
    }

    /**
     * <p>
     *     Adds the option details into container with order
     * </p>
     *
     * @param value        Represents the field value
     * @param displayValue Represents the field name
     * @param fieldOrder   Represents the field order
     * @param isEditable   Represents whether the option can be edited or not
     * @param isDefault    Represents the option is default or not
     */
    public void addOption(final Object value, final Object displayValue, final int fieldOrder, final boolean isEditable, final boolean isDefault) {
        final JsonObject option = Json.object();
        final String isDefaultKey = FieldKeys.IS_DEFAULT.getName();

        option.put(displayKeyName, displayValue);
        option.put(keyName, value);
        option.put(order, fieldOrder);
        option.put(isEditableOption, isEditable);

        if (isDefault) {
            option.put(isDefaultKey, true);
        }
        addOption(option);
    }

    /**
     * <p>
     *     Adds options into the container
     * </p>
     * @param option
     */
    public void addOption(final JsonObject option) {
        options.put(option);
    }

    /**
     * Fetches the option details
     * @return
     */
    public JsonArray getOptions() {
        return options;
    }

    /**
     * Checks whether the container has no elements or not
     * @return True, if the container has at least one element
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns the number of option elements stored by the container
     * @return
     */
    public int size() {
        return options.size();
    }

    /**
     * <p>
     * Gets the total number of records.
     * </p>
     *
     * @return The total number of records.
     */
    public long getTotalRecords() {
        return totalRecords;
    }

    /**
     * <p>
     * Sets the total number of records.
     * </p>
     *
     * @param totalRecords The total number of records to set.
     */
    public void setTotalRecords(final long totalRecords) {
        this.totalRecords = totalRecords;
    }
}
