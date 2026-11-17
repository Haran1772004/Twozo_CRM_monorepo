package com.tlc.crm.entity.service.filter;

import com.tlc.crm.entity.service.internal.FilterSourceServiceImpl;
import com.tlc.crm.fields.models.FieldSource;

import java.util.Collection;

/**
 * Provides an abstract way to deal with filter source.
 *
 * @author Kishore J
 * @version 1.0
 */
public interface FilterSourceService {

    /**
     * <p>
     * Gets the {@link FilterSourceService} instance.
     * </p>
     *
     * @return {@link FilterSourceService}.
     */
    static FilterSourceService getInstance() {
        return FilterSourceServiceImpl.getInstance();
    }

    /**
     * <p>
     * Checks whether the given collection of ids is present in the field source.
     * </p>
     *
     * @param ids Collection of field source ids.
     * @return A flag that indicates all the given ids are present or not in the field source.
     */
    boolean exists(final Collection<Long> ids);

    /**
     * <p>
     * Fetches all the available filter sources.
     * </p>
     *
     * @return Collection of {@link FieldSource}.
     */
    Collection<FieldSource> getAll();

    /**
     * <p>
     * Fetches the filter source for the given id.
     * </p>
     *
     * @param id Represents the source id.
     * @return {@link FieldSource} that holds the details of the source.
     */
    FieldSource get(final Long id);

    /**
     * <p>
     * Fetches the filter source for the given name.
     * </p>
     *
     * @param name Represents the name of the {@link FieldSource}.
     * @return {@link FieldSource} that holds the details of the source.
     */
    FieldSource getByName(final String name);
}
