package com.tlc.cdc.core.registrar;

import com.tlc.commons.service.registrar.model.RegistrarData;

import java.util.Collection;

/**
 * A model that holds the data of the {@link CdcResourceRegistrar} which is needed for registering the resources/actions.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class CdcRegistrarData extends RegistrarData {

    private Collection<Class<?>> classes;

    public void setClasses(final Collection<Class<?>> classes) {
        this.classes = classes;
    }

    public Collection<Class<?>> getClasses() {
        return classes;
    }
}
