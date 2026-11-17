package com.tlc.crm.entity.resolver;

import com.tlc.crm.entity.internal.OrderResolverImpl;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

/**
 * A resolver which resolves the order related changes
 *
 * @author KarthickV
 * @version 1.0
 */
public interface OrderResolver {

    /**
     * <p>
     * Retrieves a singleton instance of the OrderResolver implementation
     * </p>
     *
     * @return The singleton instance of the OrderResolver implementation
     */
    static OrderResolver getInstance() {
        return OrderResolverImpl.getInstance();
    }

    /**
     * <p>
     * Gets the next order that satisfies the specified criteria for the organization and table
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param whereClause Represents the criteria
     * @param table       Represents the entity table
     * @return the next order of the entity
     */
    int getNextOrder(final Long orgId, final WhereClause whereClause, final Table table);
}
