package com.tlc.crm.entity.service.list;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.service.internal.ProviderServiceImpl;

/**
 * <p>
 * Provides the abstract way to deal with {@link Provider}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface ProviderService {

    /**
     * <p>
     * Gets the {@link ProviderService} instance.
     * </p>
     *
     * @return {@link ProviderService}.
     */
    static ProviderService getInstance() {
        return ProviderServiceImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link Provider} entity for the given entity id.
     * </p>
     *
     * @param id Represents the provider id
     * @return {@link Provider} entity.
     */
    Provider get(final Long id);

    /**
     * <p>
     * Fetches the {@link Provider} entity for given source id.
     * </p>
     *
     * @param sourceId Represents the source id.
     * @return {@link Provider} entity.
     */
    Provider getBySourceId(final Long sourceId);

    /**
     * <p>
     * Fetches the provider id for given source id.
     * </p>
     *
     * @param sourceId Represents the source id
     * @return Provider id.
     */
    Long getIdBySourceId(final Long sourceId);

    /**
     * <p>
     * Fetches the id of the provider by {@link Source}.
     * </p>
     *
     * @param source Represents the source.
     * @return Provider id.
     */
    Long getIdBySource(final Source source);

    /**
     * <p>
     * Fetches the id of the provider.
     * </p>
     *
     * @param provider Represents the provider.
     * @return Provider id.
     */
    Long getIdByProvider(final Provider provider);
}