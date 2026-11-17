package com.tlc.iam.server.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.iam.common.models.ClientApplication;
import com.tlc.iam.common.status.IamErrorCodes;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A registry that is used to registers the application details
 *
 * @author Selvakumar
 * @version 1.0
 */
public class ApplicationRegistry {

    private final Map<String, ClientApplication> productsHolder;

    private ApplicationRegistry() {
        this.productsHolder = new HashMap<>();
    }

    private static final class Instance {
        private static final ApplicationRegistry APPLICATION_REGISTRY = new ApplicationRegistry();
    }

    /**
     * <p>
     * Gets the instance of {@link ApplicationRegistry}
     * </p>
     *
     * @return {@link ApplicationRegistry}
     */
    public static ApplicationRegistry getInstance() {
        return Instance.APPLICATION_REGISTRY;
    }

    /**
     * Checks whether the given app is registered or not.
     *
     * @param name Application name
     * @return true if it is registered
     */
    public boolean contains(final String name) {
        return productsHolder.containsKey(name);
    }

    /**
     * Checks whether the given app is registered or not.
     *
     * @param application Holds application details
     * @return true if it is registered
     */
    public boolean contains(final ClientApplication application) {
        return productsHolder.containsKey(application.name());
    }

    /**
     * Fetches the registered application
     *
     * @param applicationName Application name
     * @return If it found any registered application, identified value will be returned
     * or else it will throw an exception
     */
    public ClientApplication get(final String applicationName) {
        final ClientApplication application = productsHolder.get(applicationName);

        if (Objects.nonNull(application)) {
            return application;
        }

        throw ErrorCode.get(IamErrorCodes.INVALID_APP_NAME, "i18n.invalid.app.name");
    }

    /**
     * Returns the number of applications registered
     *
     * @return Number of applications registered
     */
    public int size() {
        return productsHolder.size();
    }

    /**
     * Registers the application
     *
     * @param application Holds the application details
     */
    public void add(final ClientApplication application) {
        productsHolder.put(application.name(), application);
    }

    /**
     * Removes the application from the registry
     *
     * @param name Application name
     * @return Removed application
     */
    public ClientApplication remove(final String name) {
        return productsHolder.remove(name);
    }

    /**
     * Removes the application from the registry
     *
     * @param application Holds the application details
     * @return Removed application
     */
    public ClientApplication remove(final ClientApplication application) {
        return productsHolder.remove(application.name());
    }

    /**
     * Clears all the application details from the registry
     */
    public void clear() {
        productsHolder.clear();
    }

}
