package com.tlc.crm.entity.service.filter;

import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.service.internal.ComparatorServiceImpl;

import java.util.Collection;

/**
 * <p>
 * Provides an abstract way to deal with {@link Comparator}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface ComparatorService {

    /**
     * <p>
     * Gets the {@link ComparatorService} instance.
     * </p>
     *
     * @return {@link ComparatorService}.
     */
    static ComparatorService getInstance() {
        return ComparatorServiceImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link Comparator} entity given by id.
     * </p>
     *
     * @param id Represents the comparator id.
     * @return {@link Comparator} entity.
     */
    Comparator get(final Long id);

    /**
     * <p>
     * Fetches the {@link Comparator} entity for the specified name.
     * </p>
     *
     * @param name Represents the name of the {@link Comparator}.
     * @return {@link Comparator} entity.
     */
    Comparator getByName(final String name);

    /**
     * <p>
     * Fetches the collection of {@link Comparator} given by field type id.
     * </p>
     *
     * @param fieldTypeId Represents the field type id.
     * @return Collection of {@link Comparator} entity.
     */
    Collection<Comparator> getComparatorsByType(final Long fieldTypeId);

    /**
     * <p>
     * Fetches the collection of {@link Comparator} given by field subtype id.
     * </p>
     *
     * @param fieldSubTypeId Represents the field subtype id.
     * @return Collection of {@link Comparator} entity.
     */
    Collection<Comparator> getComparatorsBySubType(final Long fieldSubTypeId);

    /**
     * <p>
     * Checks whether the comparators present or not for the given collection of ids.
     * </p>
     *
     * @param comparatorIds Collection of comparator ids.
     * @return If the comparator details exist it will return true, otherwise it will return false.
     */
    boolean exists(final Collection<Long> comparatorIds);
}
