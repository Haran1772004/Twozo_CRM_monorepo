package com.tlc.crm.core.features.api.dataimport.model;

/**
 * Describes the type of alteration carried out in a database when importing a record
 *
 * @author sundar
 * @version 1.0
 */
public enum ImportType {

    INSERT("insert", (short) 1),
    UPDATE("update", (short) 2),
    MERGE("merge", (short) 3);

    private final String name;
    private final short id;

    ImportType(final String name, final short id) {
        this.name = name;
        this.id = id;
    }

    /**
     * Gets the name of the type
     *
     * @return name of the type
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the ID of the {@link ImportType}
     *
     * @return the ID of the {@link ImportType}
     */
    public int getId() {
        return id;
    }
}
