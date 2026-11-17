package com.tlc.crm.fields.service.internal;

import com.tlc.crm.fields.models.FieldStore;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.resolver.FieldStoreResolver;
import com.tlc.crm.fields.service.FieldStoreService;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implements the {@link FieldStoreService} provides functionality for accessing {@link FieldStore}.
 *
 * @author Kishore J
 * @version 1.0
 * @see FieldStoreService
 */
public class FieldStoreServiceImpl implements FieldStoreService {

    private final FieldStoreResolver fieldStoreResolver;

    private static class Instance {
        private static final FieldStoreServiceImpl INSTANCE = new FieldStoreServiceImpl();
    }

    private FieldStoreServiceImpl() {
        fieldStoreResolver = FieldStoreResolver.getInstance();
    }

    /**
     * <p>
     * Gets the instance of  {@link FieldStoreServiceImpl}.
     * </p>
     *
     * @return {@link FieldStoreServiceImpl}.
     */
    public static FieldStoreServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the store id.
     * @return {@link FieldStore}
     */
    @Override
    public FieldStore getFieldStoreById(final Long id) {
        return fieldStoreResolver.get(id);
    }

    /**
     * <p>
     * Fetches the field store details for the given collection of field store ids.
     * </p>
     *
     * @param ids Represents the collection of field store ids.
     * @return {@link Collection} of {@link FieldStore}
     */
    @Override
    public Collection<FieldStore> getFieldStoresByIds(final Collection<Long> ids) {
        return fieldStoreResolver.getAll().stream().filter(Objects::nonNull)
                .filter(fieldStore -> ids.contains(fieldStore.id())).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceName represents the source name
     * @param type       represents the store type
     * @return @return {@link FieldStore}
     */
    @Override
    public FieldStore getFieldStoreByType(final String sourceName, final StoreType type) {
        return getFieldStoreByType(sourceName, type, false);
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceName   represents the source name
     * @param type         represents the store type
     * @param isPersistent whether to fetch the persistent or non-persistent store
     * @return {@link FieldStore}
     */
    @Override
    public FieldStore getFieldStoreByType(final String sourceName, final StoreType type, final boolean isPersistent) {
        final Optional<FieldStore> fieldStore = fieldStoreResolver.getFieldStoresBySource(sourceName).stream()
                .filter(crmFieldStore -> Objects.equals(crmFieldStore.storeType(), type) && crmFieldStore.isPersistent() == isPersistent)
                .findFirst();

        return fieldStore.orElse(null);
    }
}