package com.tlc.analytics.general.trash;

import com.tlc.pk.trash.db.DBBackedTrashManager;
import com.tlc.pk.trash.db.TrashEntity;

import lombok.NonNull;

/**
 * Abstract base class for a specialized trash manager with database-backed functionality.
 * This class extends DBBackedTrashManager and provides shared logic or configurations
 * for managing trash entities across multiple service implementations.
 *
 * @param <I> the type of trash entity managed by this class
 */
public abstract class BaseTrashManager<I extends TrashEntity> extends DBBackedTrashManager<I>
{
    /**
     * Provides the unique identifier for the service owning the trash items.
     * This implementation returns a constant value but can be overridden by subclasses
     * to represent different services uniquely.
     *
     * @return the service ID
     */
    @Override
    protected @NonNull Long getServiceId()
    {
        return 1L; // Default service ID
    }
}
