package com.tlc.crm.entity.service.internal;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.resolver.list.ProviderResolver;
import com.tlc.crm.entity.service.list.ProviderService;

import java.util.Map;
import java.util.Optional;

/**
 * Implements the {@link ProviderService} provides functionality for accessing provider objects.
 *
 * @author Kishore J
 * @version 1.0
 * @see ProviderService
 */
public class ProviderServiceImpl implements ProviderService {

    private final ProviderResolver providerResolver;

    private ProviderServiceImpl() {
        providerResolver = ProviderResolver.getInstance();
    }

    private static class Instance {
        private static final ProviderServiceImpl INSTANCE = new ProviderServiceImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link ProviderServiceImpl}.
     * </p>
     *
     * @return {@link ProviderServiceImpl}.
     */
    public static ProviderServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the provider id
     * @return {@link Provider} entity.
     */
    @Override
    public Provider get(final Long id) {
        return providerResolver.getAll().get(id);
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceId Represents the source id.
     * @return {@link Provider} entity.
     */
    @Override
    public Provider getBySourceId(final Long sourceId) {
        return providerResolver.getProvider(sourceId).getValue();
    }

    /**
     * <p>
     * Fetches the provider id for given source id.
     * </p>
     *
     * @param sourceId Represents the source id.
     * @return Provider id.
     */
    @Override
    public Long getIdBySourceId(final Long sourceId) {
        return providerResolver.getProvider(sourceId).getKey();
    }

    /**
     * <p>
     * Fetches the id of the provider by {@link Source}.
     * </p>
     *
     * @param source Represents the source.
     * @return Provider id.
     */
    @Override
    public Long getIdBySource(final Source source) {
        final Optional<Long> optionalProvider = providerResolver.getAll().entrySet().stream()
                .filter(providerEntry -> source == providerEntry.getValue().getSource()).map(Map.Entry::getKey).findFirst();

        return optionalProvider.orElseGet(() -> null);
    }

    /**
     * <p>
     * Fetches the id of the provider.
     * </p>
     *
     * @param provider Represents the provider details.
     * @return Provider id.
     */
    @Override
    public Long getIdByProvider(final Provider provider) {
        final Optional<Map.Entry<Long, Provider>> optionalProviderEntry = providerResolver.getAll().entrySet().stream().filter(providerEntry -> provider == providerEntry.getValue()).findAny();

        return optionalProviderEntry.map(Map.Entry::getKey).orElseGet(() -> null);
    }
}