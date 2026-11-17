package com.tlc.crm.entity.service.internal;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.internal.status.FilterErrorCodes;
import com.tlc.crm.entity.resolver.filter.ComparatorResolver;
import com.tlc.crm.entity.service.filter.ComparatorService;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Implements the {@link ComparatorService} provides functionality for accessing {@link Comparator}.
 *
 * @author Kishore J
 * @version 1.0
 * @see ComparatorService
 */
public class ComparatorServiceImpl implements ComparatorService {

    private final ComparatorResolver comparatorResolver;

    private static class Instance {
        private static final ComparatorServiceImpl INSTANCE = new ComparatorServiceImpl();
    }

    private ComparatorServiceImpl() {
        this.comparatorResolver = ComparatorResolver.getInstance();
    }

    /**
     * <p>
     * Gets the instance of {@link ComparatorServiceImpl}.
     * </p>
     *
     * @return {@link ComparatorServiceImpl}.
     */
    public static ComparatorServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the comparator id.
     * @return {@link Comparator} entity.
     */
    @Override
    public Comparator get(final Long id) {
        final Comparator comparator = comparatorResolver.getComparators().get(id);

        if (Objects.isNull(comparator)) {
            throw ErrorCode.get(FilterErrorCodes.COMPARATOR_NOT_FOUND);
        }

        return comparator;
    }

    /**
     * {@inheritDoc}
     *
     * @param name Represents the name of the {@link Comparator}.
     * @return {@link Comparator} entity.
     */
    @Override
    public Comparator getByName(final String name) {
        for (final Map.Entry<Long, Comparator> comparatorEntry : comparatorResolver.getComparators().entrySet()) {
            final Comparator comparator = comparatorEntry.getValue();

            if (comparator.getAliasName().equalsIgnoreCase(name)) {
                return comparator;
            }
        }
        throw ErrorCode.get(FilterErrorCodes.COMPARATOR_NOT_FOUND);
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId Represents the field type id.
     * @return Collection of {@link Comparator} entity.
     */
    @Override
    public Collection<Comparator> getComparatorsByType(final Long fieldTypeId) {
        return comparatorResolver.getComparatorsByType(fieldTypeId);
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldSubTypeId Represents the field subtype id.
     * @return Collection of {@link Comparator} entity.
     */
    @Override
    public Collection<Comparator> getComparatorsBySubType(final Long fieldSubTypeId) {
        return comparatorResolver.getComparatorsBySubType(fieldSubTypeId);
    }

    /**
     * {@inheritDoc}
     *
     * @param comparatorIds Collection of comparator ids.
     * @return If the comparator details exist it will return true, otherwise it will return false.
     */
    @Override
    public boolean exists(final Collection<Long> comparatorIds) {
        final Map<Long, Comparator> comparators = comparatorResolver.getComparators();

        for (final Long comparatorId : comparatorIds) {
            if (!comparators.containsKey(comparatorId)) {
                return false;
            }
        }
        return true;
    }
}