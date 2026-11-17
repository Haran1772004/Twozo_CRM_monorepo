package com.tlc.crm.common.export.model;

/**
 * <p>
 * Holds categories of data exports.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public enum ExportCategory {

    FILE((short) 1, "export/file"),
    ;

    private final short id;
    private final String type;

    ExportCategory(final short id, final String type) {
        this.id = id;
        this.type = type;
    }

    public short getId() {
        return id;
    }

    public String getType() {
        return type;
    }
}
