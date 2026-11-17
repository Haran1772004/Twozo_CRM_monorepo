package com.tlc.crm.entity.filter.api.manager;

import com.tlc.crm.fields.manager.AbstractReadableConfigManager;

import com.tlc.crm.entity.filter.api.manager.impl.ComparatorManagerImpl;
import com.tlc.crm.entity.filter.api.models.Comparator;

import java.util.Collection;

/**
 * <p>
 * Provides the {@link Comparator} related DB fetch action.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see AbstractReadableConfigManager
 */
public interface ComparatorManager {

    static ComparatorManager getInstance() {
        return ComparatorManagerImpl.getInstance();
    }

    /**
     * <p>
     * Fetches all the {@link Comparator}.
     * </p>
     *
     * @return collection of {@link Comparator} instance that holds the retrieved values.
     */
    Collection<Comparator> getAll();

    /**
     * <p>
     * Gets the collection of comparators for the given field type id.
     * </p>
     *
     * @param fieldTypeId Represents the field type id
     * @return The {@link Comparator}
     */
    Collection<Comparator> getComparatorsByType(final Long fieldTypeId);

    /**
     * <p>
     * Gets the collection of comparators for the given field subtype id.
     * </p>
     *
     * @param fieldSubTypeId Represents the field subtype id
     * @return The {@link Comparator}
     */
    Collection<Comparator> getComparatorsBySubType(final Long fieldSubTypeId);
}
