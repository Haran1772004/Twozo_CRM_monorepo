package com.tlc.crm.entity.handler;

import com.tlc.commons.Counter;
import com.tlc.crm.entity.internal.OrderManagerImpl;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;

/**
 * <p>
 * The handler which handles the order related update event
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public interface OrderManager {

    /**
     * <p>
     * Retrieves a singleton instance of the OrderManager implementation
     * </p>
     *
     * @return The singleton instance of the OrderResolver implementation
     */
    static OrderManager getInstance() {
        return OrderManagerImpl.getInstance();
    }

    /**
     * <p>
     * Updates the specified records in the given table
     * </p>
     *
     * @param orgId   Represents the organization id
     * @param counter Represents the initial order value
     * @param ids     Represents the collection of entity ids
     * @param table   Represents the entity table
     */
    void update(final Long orgId, final Counter counter, final Collection<Long> ids, final Table table);
}
