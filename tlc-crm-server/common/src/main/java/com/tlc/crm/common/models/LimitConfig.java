package com.tlc.crm.common.models;

import com.tlc.sql.common.api.dml.LimitClause;

/**
 * Represents pagination details with a start index and limit.
 * <p>
 * This record is used to manage paginated queries, ensuring that results
 * are retrieved in a structured manner.
 * </p>
 *
 * @param start The starting index for the paginated data.
 * @param limit The maximum number of records to fetch.
 *
 * @author Sathish
 * @version 1.0
 */
public record LimitConfig(int start, int limit) {

    public LimitClause getLimitClause() {
        return new LimitClause(start, limit);
    }
}
