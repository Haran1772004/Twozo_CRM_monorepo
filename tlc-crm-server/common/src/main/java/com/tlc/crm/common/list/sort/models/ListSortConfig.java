package com.tlc.crm.common.list.sort.models;

import com.tlc.crm.common.list.model.ListType;

/**
 * <p>
 * A model that represents the list sort config data
 * </p>
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public class ListSortConfig extends SortConfig {

    private ListType listType;

    public ListSortConfig(final Long orgId, final Long userId, final Long sourceId) {
        super(orgId, userId, sourceId);
    }

    public ListType getListType() {
        return listType;
    }

    public void setListType(final ListType listType) {
        this.listType = listType;
    }
}