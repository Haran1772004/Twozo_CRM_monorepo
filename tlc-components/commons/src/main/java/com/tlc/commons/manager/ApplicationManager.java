package com.tlc.commons.manager;

import com.tlc.commons.model.Application;

/**
 * Manages the DB related actions of the {@link Application}.
 *
 * @param <A> The type of Application managed
 * @author Sathish
 * @version 1.0
 */
public interface ApplicationManager<A extends Application> {

    /**
     * Retrieves an Application by its unique identifier.
     *
     * @param id The unique id of the Application
     * @return The Application instance corresponding to the given id, or null if not found
     */
    A get(final long id);

    /**
     * Retrieves an Application by its name.
     *
     * @param appName The name of the Application
     * @return The Application instance corresponding to the given name, or null if not found
     */
    A getByName(final String appName);
}
