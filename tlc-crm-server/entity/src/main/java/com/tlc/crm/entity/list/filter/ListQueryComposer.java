package com.tlc.crm.entity.list.filter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;

/**
 * ListQueryComposer is responsible for enhancing or modifying list queries
 * by dynamically appending additional clauses such as SELECT, JOIN, WHERE, and ORDER BY.
 * <p>
 * This class is typically used in scenarios where the base query needs to be
 * extended based on runtime configurations, filters, or field definitions.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 *     ListQueryComposer augmentor = new ListQueryComposer();
 *     augmentor.addJoin(...);
 *     augmentor.addWhere(...);
 *     augmentor.addOrderBy(...);
 * </pre>
 *
 * @author Sathish
 * @version 1.0
 */
public interface ListQueryComposer {

    /**
     * <p>
     * Selects the additional fields which is needed for the list response.
     * </p>
     *
     * @param fieldConfig Holds the details of the field.
     * @param selectQuery The data retrieval query.
     */
    void select(final FieldConfig fieldConfig, final SelectQuery selectQuery);

    /**
     * Constructs the JOIN clause based on the provided field configuration.
     *
     * @param fieldConfig The configuration describing the field to join on.
     * @param selectQuery The selectQuery where the  join clause is added on.
     */
    default void loadJoinClause(final FieldConfig fieldConfig, final SelectQuery selectQuery) {
        throw ErrorCode.get(EntityErrorCodes.NOT_SUPPORTED, "i18n.entity.list.field.join.unsupported");
    }

    /**
     * Builds the ORDER BY clause for the given field and sort direction.
     *
     * @param fieldConfig The field configuration to sort by.
     * @param orderType   The type of ordering (ASC or DESC).
     * @return An OrderByClause representing the ORDER BY expression.
     */
    default OrderByClause getOrderByClause(final FieldConfig fieldConfig, final OrderByClause.OrderType orderType) {
        throw ErrorCode.get(EntityErrorCodes.NOT_SUPPORTED, "i18n.entity.list.field.sort.unsupported");
    }
}
