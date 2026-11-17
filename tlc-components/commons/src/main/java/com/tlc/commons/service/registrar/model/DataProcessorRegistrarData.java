package com.tlc.commons.service.registrar.model;

import com.tlc.commons.service.registrar.DataProcessorRegistrar;

import java.util.Collection;

/**
 * <p>
 * A model that holds the data of the {@link DataProcessorRegistrar} which is needed for registering the actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class DataProcessorRegistrarData extends RegistrarData {

    private Collection<Class<?>> classes;

    public void setClasses(final Collection<Class<?>> classes) {
        this.classes = classes;
    }

    public Collection<Class<?>> getClasses() {
        return classes;
    }
}
