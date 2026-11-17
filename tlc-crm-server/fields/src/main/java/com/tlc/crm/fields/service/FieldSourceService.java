package com.tlc.crm.fields.service;

import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.service.internal.FieldSourceServiceImpl;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides an abstract way to deal with {@link FieldSource}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface FieldSourceService {

    /**
     * <p>
     * Gets the {@link FieldSourceService} instance.
     * </p>
     *
     * @return {@link FieldSourceService}.
     */
    static FieldSourceService getInstance() {
        return FieldSourceServiceImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the source entity by its name
     * </p>
     *
     * @param sourceName Name of the source
     * @return {@link FieldSource}
     */
    FieldSource getSource(final String sourceName);

    /**
     * <p>
     * Fetches the source entity by id
     * </p>
     *
     * @param id Field source id
     * @return {@link FieldSource}
     */
    FieldSource getSource(final Long id);

    /**
     * <p>
     * Fetches the collection of field source entities for the given collection of field source ids.
     * </p>
     *
     * @param ids Collection of field source ids
     * @return Collection of field source entities
     */
    Collection<FieldSource> getFieldSourcesByIds(final Collection<Long> ids);

    /**
     * <p>
     * Gets the field source with its settings details.
     * </p>
     *
     * @param orgId      Refers the organization id.
     * @param sourceName Refers the source name.
     * @return Field source.
     */
    FieldSource retrieveSourceWithSettings(final Long orgId, final String sourceName);

    /**
     * <p>
     * Fetches the collection of field source entities.
     * </p>
     *
     * @return Collection of field source entities
     */
    Collection<FieldSource> getSources(final Collection<String> sourceNames);

    /**
     * <p>
     * Fetches the collection of field source entities.
     * </p>
     *
     * @return Collection of field source entities
     */
    Collection<FieldSource> getSources();

    /**
     * <p>
     * Fetches the group of field sources.
     * </p>
     *
     * @return The {@link Map} containing key as field source id value as {@link FieldSource}.
     */
    Map<Long, FieldSource> groupSources();
}