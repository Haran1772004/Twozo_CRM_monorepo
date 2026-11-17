package com.tlc.crm.common.list.pagination;

import com.tlc.crm.common.action.list.pagination.PaginationState;
import com.tlc.crm.common.list.model.ListType;

/**
 * Represents the pagination state for a list view.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class ListPaginationState extends PaginationState {

    private ListType listType;

    public ListPaginationState(final Long orgId, final Long userId) {
        super(orgId, userId);
    }

    public ListPaginationState(final Long orgId, final Long userId, final ListType listType) {
        super(orgId, userId);
        this.listType = listType;
    }

    public ListType getListType() {
        return listType;
    }

    public void setListType(final ListType listType) {
        this.listType = listType;
    }
}
