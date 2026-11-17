package com.tlc.crm.entity.models.type;

/**
 * <p>
 * Represents the type of dropdown using options
 * </p>
 *
 * @author NandhiniRakAS
 * @version 1.0
 */
public enum DropdownType {
    STATIC,
    DYNAMIC,
    HYBRID,
    HIDDEN_OPTION,
    GROUPED;

    /**
     * Determines if the options of current dropdown type are mutable
     *
     * @return true if the dropdown type's options can be modified, false otherwise
     */
    public boolean isMutable() {

        return switch (this) {
            case HYBRID, DYNAMIC -> true;
            default -> false;
        };
    }

    /**
     * <p>
     * Determines if the current option represents a hidden option
     * </p>
     *
     * @return true if the current option is  hidden option, otherwise false
     */
    public boolean isHiddenOption() {
        return HIDDEN_OPTION == this;
    }
}
