package com.tlc.crm.entity.list.filter.appender.model;

import com.tlc.sql.common.api.dml.Column;


/**
 * Abstract base class that defines the core requirement for any criteria-based filter logic,
 * primarily associated with a specific SQL {@link Column}.
 *
 * <p>
 * Subclasses of this class are expected to represent different filtering strategies or types
 * that operate on a given column as part of list filtering or query construction.
 * </p>
 *
 * <p>Example use cases:
 * <ul>
 *     <li>Appending positive or negative filtering conditions</li>
 *     <li>Building dynamic where clauses</li>
 * </ul>
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class CriteriaAppenderRequisites {

    private final Column column;

    protected CriteriaAppenderRequisites(final Column column) {
        this.column = column;
    }

    public Column getColumn() {
        return column;
    }
}
