package com.tlc.crm.common.action.list.pagination;

import com.tlc.validator.TlcModel;

/**
 * Represents the persisted pagination state for a user and view. Stores the current page offset and
 * page size so that the user's pagination state can be restored across requests.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public class PaginationState implements TlcModel {

    private Long id;
    protected int start;
    protected int count;
    private final Long orgId;
    private final Long userId;

    public PaginationState(final Long orgId, final Long userId) {
        this.orgId = orgId;
        this.userId = userId;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    public Long getUserId() {
        return userId;
    }

    public int getStart() {
        return start;
    }

    public void setStart(final int start) {
        this.start = start;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public int getCount() {
        return count;
    }

    public void setCount(final int count) {
        this.count = count;
    }

}
