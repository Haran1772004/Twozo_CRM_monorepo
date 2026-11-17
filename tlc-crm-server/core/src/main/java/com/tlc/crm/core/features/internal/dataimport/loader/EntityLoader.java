package com.tlc.crm.core.features.internal.dataimport.loader;

import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.validator.TlcModel;

/**
 * An abstract loader to load the {@link com.tlc.crm.entity.models.Entity} into the {@link SourceModelContainer}.
 *
 * @param <T> Represents the model that should be derived from {@link TlcModel}.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class EntityLoader<T extends TlcModel> extends AbstractModelLoader<T> {

    protected EntityLoader(Source source) {
        super(source);
    }

    /**
     * Clones the given entity by fetch its summary details.
     *
     * @param orgId  Refers the organization id.
     * @param entity Refers the entity.
     * @return the cloned entity
     */
    @Override
    protected T cloneModel(final long orgId, final T entity) {
        final T oldModel = super.cloneModel(orgId, entity);

        loadEntitySummary(orgId, oldModel);
        return oldModel;
    }

    /**
     * Loads the summary details into the entity.
     *
     * @param orgId  Refers the organization id.
     * @param entity Refers the entity.
     */
    protected abstract void loadEntitySummary(final long orgId, final T entity);
}
