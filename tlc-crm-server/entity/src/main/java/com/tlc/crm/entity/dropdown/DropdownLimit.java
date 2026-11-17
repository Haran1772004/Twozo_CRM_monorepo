package com.tlc.crm.entity.dropdown;

import com.tlc.sql.common.api.dml.LimitClause;

/**
 * Holds the {@link LimitClause} details for dropdown actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public record DropdownLimit(int start, int limit) {

    /**
     * Returns the {@link LimitClause}.
     *
     * @return {@link LimitClause}.
     */
    public LimitClause getLimitClause() {
        return new LimitClause(Math.max(1, start), Math.min(100, limit));
    }

}
