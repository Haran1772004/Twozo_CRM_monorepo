package com.tlc.crm.entity.dropdown;

/**
 * An interface holds the default options
 *
 * @author Kishore J
 * @version 1.0
 */
public interface DefaultOption {

    /**
     * Gets the name of the option
     *
     * @return The option name
     */
    String getName();

    /**
     * <p>
     * Determines whether the option is editable
     * </p>
     *
     * @return true If the option is editable, otherwise it will return false
     */
    boolean isEditable();

    /**
     * <p>
     * Determines whether the option is default option or not
     * </p>
     *
     * @return true If the option is default, otherwise it will return false
     */
    boolean isDefault();
}