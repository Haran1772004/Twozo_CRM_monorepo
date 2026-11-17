package com.tlc.crm.notifications.internal.loader.phone.impl;

import com.tlc.commons.page.Page;
import com.tlc.crm.notifications.api.call.manager.PhoneNumberManager;
import com.tlc.crm.notifications.api.call.model.Locality;
import com.tlc.crm.notifications.internal.loader.phone.PhoneNumberLocationLoader;
import com.tlc.crm.notifications.page.PhoneNumberLocationPageHandler;

import java.util.Collection;
import java.util.HashSet;

/**
 * Implementation of {@link PhoneNumberLocationLoader} that handles loading and unloading
 * of phone number location data from a voice call service.
 * <p>
 * This class is responsible for fetching available countries, states, and localities
 * from the voice call service and storing them in the database through {@link PhoneNumberManager}.
 * </p>
 *
 * <p>
 * This class follows the Singleton design pattern.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class PhoneNumberLocationLoaderImpl implements PhoneNumberLocationLoader {

    private final PhoneNumberManager phoneNumberManager;

    /**
     * Private constructor to enforce Singleton pattern.
     */
    private PhoneNumberLocationLoaderImpl() {
        phoneNumberManager = PhoneNumberManager.getInstance();
    }

    /**
     * Retrieves the singleton instance of {@link PhoneNumberLocationLoaderImpl}.
     *
     * @return The singleton instance.
     */
    public static PhoneNumberLocationLoaderImpl getInstance() {
        return InstanceHolder.PHONE_NUMBER_META_LOADER;
    }

    /**
     * Static nested class for Singleton instance.
     */
    private static final class InstanceHolder {
        private static final PhoneNumberLocationLoaderImpl PHONE_NUMBER_META_LOADER = new PhoneNumberLocationLoaderImpl();
    }

    /**
     * Loads phone number location data in multiple iterations.
     * <p>
     * This method fetches available countries and their localities from the voice call service
     * and stores them in the database through {@link PhoneNumberManager}.
     * </p>
     *
     * @param iteration The number of iterations to run when loading data in batches.
     */
    @Override
    public void load(final int iteration) {
        final Collection<Locality> localities = new HashSet<>();
        final PhoneNumberLocationPageHandler locationPageHandler = new PhoneNumberLocationPageHandler((long) iteration);

        for (;locationPageHandler.hasNextPage(); locationPageHandler.goToNextPage()) {
            final Page<Locality> currentPage = locationPageHandler.getCurrentPage();

            localities.addAll(currentPage.getData());
        }

        if (localities.isEmpty()) {
            return;
        }
        phoneNumberManager.storeLocations(localities);
    }

    /**
     * Unloads phone number location data.
     */
    @Override
    public void unload() {
        // Currently we don't need to unload any data.
        // TODO If it's needed in the future, we can implement this.
    }
}