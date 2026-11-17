package com.tlc.crm.entity.service.internal;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.entity.internal.status.FilterErrorCodes;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.entity.resolver.filter.FilterSourceResolver;
import com.tlc.crm.entity.service.filter.FilterSourceService;

import java.util.Collection;

/**
 * Implements the {@link FilterSourceService} provides functionality for accessing filter source.
 *
 * @author Kishore J
 * @version 1.0
 * @see FilterSourceService
 */
public class FilterSourceServiceImpl implements FilterSourceService {

    private final FilterSourceResolver filterSourceResolver;

    private FilterSourceServiceImpl() {
        filterSourceResolver = FilterSourceResolver.getInstance();
    }

    private static class Instance {
        private static final FilterSourceServiceImpl INSTANCE = new FilterSourceServiceImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link FilterSourceServiceImpl}.
     * </p>
     *
     * @return {@link FilterSourceServiceImpl}.
     */
    public static FilterSourceServiceImpl getInstance() {
        return Instance.INSTANCE;
    }


    /**
     * {@inheritDoc}
     *
     * @param ids Collection of field source ids.
     * @return A flag that indicates all the given ids are present or not in the field source.
     */
    @Override
    public boolean exists(final Collection<Long> ids) {
        return filterSourceResolver.exists(ids);
    }

    /**
     * {@inheritDoc}
     *
     * @return Collection of {@link FieldSource}.
     */
    @Override
    public Collection<FieldSource> getAll() {
        return filterSourceResolver.getAll();
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the source id
     * @return {@link FieldSource} that holds the details of the source.
     */
    @Override
    public FieldSource get(final Long id) {
        return filterSourceResolver.get(id);
    }

    /**
     * <p>
     * Fetches the filter source for the given name.
     * </p>
     *
     * @param name Represents the name of the {@link FieldSource}.
     * @return {@link FieldSource} that holds the details of the source.
     */
    @Override
    public FieldSource getByName(final String name) {
        for (final FieldSource source : filterSourceResolver.getAll()) {
            if (source.getName().equalsIgnoreCase(name)) {
                return source;
            }
        }
        throw ErrorCode.get(FilterErrorCodes.FIELD_SOURCE_ID_NOT_FOUND);
    }
}