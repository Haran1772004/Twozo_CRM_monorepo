package com.tlc.crm.entity.handler;

import com.tlc.crm.entity.dropdown.DefaultOption;
import com.tlc.crm.entity.internal.DefaultOptionHandlerImpl;

import java.util.Collection;

/**
 * Handler which provides abstract way to deal with default option
 *
 * @author Kishore J
 * @version 1.0
 */
public interface DefaultOptionHandler {

    /**
     * <p>
     * Gets the {@link DefaultOptionHandler} instance
     * </p>
     *
     * @return The {@link DefaultOptionHandler}
     */
    static DefaultOptionHandler getInstance() {
        return DefaultOptionHandlerImpl.getInstance();
    }

    /**
     * <p>
     * Creates the options for the given org id, field id and default options
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param fieldId        Represents the field id
     * @param defaultOptions Which contains the collection of default options
     */
    void create(final long orgId, final long fieldId, final Collection<DefaultOption> defaultOptions);
}
