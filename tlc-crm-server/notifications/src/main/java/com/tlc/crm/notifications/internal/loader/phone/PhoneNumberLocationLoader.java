package com.tlc.crm.notifications.internal.loader.phone;

import com.tlc.crm.notifications.internal.loader.phone.impl.PhoneNumberLocationLoaderImpl;

/**
 * Interface for loading and unloading phone number location data.
 * <p>
 * Implementations of this interface handle the loading and unloading of phone number location data,
 * typically for caching or database purposes.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PhoneNumberLocationLoader {

    /**
     * Retrieves the singleton instance of {@link PhoneNumberLocationLoader}.
     *
     * @return The singleton instance of {@link PhoneNumberLocationLoader}.
     */
    static PhoneNumberLocationLoader getInstance() {
        return PhoneNumberLocationLoaderImpl.getInstance();
    }

    /**
     * Loads phone number location data in multiple iterations.
     * <p>
     * This method is useful for batch processing where data is loaded in chunks to prevent
     * excessive memory usage or database load.
     * </p>
     *
     * @param iteration The iteration count defining how many batches to process.
     */
    void load(final int iteration);

    /**
     * Unloads the phone number location data.
     * <p>
     * Typically used to clear cached data or reset the state after processing.
     * </p>
     */
    void unload();
}