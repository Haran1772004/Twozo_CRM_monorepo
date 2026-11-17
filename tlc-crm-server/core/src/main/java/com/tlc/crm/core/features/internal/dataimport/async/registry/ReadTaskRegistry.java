package com.tlc.crm.core.features.internal.dataimport.async.registry;

import com.tlc.crm.core.features.internal.dataimport.async.registry.impl.ReadTaskRegistryImpl;
import com.tlc.crm.core.features.internal.dataimport.async.task.ImportableFileReadTask;

/**
 * A registry that stores the allocated tasks for reading the uploaded files for the import process
 *
 * @author sundar
 * @version 1.0
 */
public interface ReadTaskRegistry {

    /**
     * Gets the {@link ReadTaskRegistryImpl} instance
     *
     * @return {@link ReadTaskRegistryImpl}
     */
    static ReadTaskRegistry get() {
        return ReadTaskRegistryImpl.getInstance();
    }

    /**
     * Gets the {@link ImportableFileReadTask} of provided request ID
     *
     * @param importId Represents the import ID
     * @return {@link ImportableFileReadTask}
     */
    ImportableFileReadTask get(final long importId);

    /**
     * Adds the provided {@link ImportableFileReadTask} into the repository associated with the given import ID.
     * Returns true if the task is successfully added to the repo. If the task already exists within the repo, it won't be added to the repository, and the method returns false.
     *
     * @param importId               Represents the import ID
     * @param importableFileReadTask Which contains import information and record holder
     * @return true if the task is successfully added to the repository otherwise false
     */
    boolean add(final long importId, final ImportableFileReadTask importableFileReadTask);

    /**
     * Removes the {@link ImportableFileReadTask} for the given read identifier
     *
     * @param importId Represents the import ID
     */
    void remove(final long importId);
}
