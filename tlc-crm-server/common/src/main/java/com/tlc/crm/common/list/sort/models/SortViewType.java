package com.tlc.crm.common.list.sort.models;

/**
 * An enum contains the sort view type
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public enum SortViewType {

    KANBAN_SORT("kanbanSort", 1),
    LIST_SORT("listSort", 2),
    ;

    private final String name;
    private final int id;

    SortViewType(final String name, final int id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }
}