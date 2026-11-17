package com.tlc.crm.security.registrar.model;

import com.tlc.commons.service.registrar.model.RegistrarData;

import java.util.Collection;

/**
 * <p>
 * A model that holds the data of the {@link com.tlc.crm.security.registrar.SecurityResourceRegistrar} which is needed for registering the resources/actions.
 * </p>
 *
 * @author sundar
 * @version 1.0
 */
public class SecurityRegistrarData extends RegistrarData {

    private Collection<Class<?>> classes;

    public void setClasses(final Collection<Class<?>> classes) {
        this.classes = classes;
    }

    public Collection<Class<?>> getClasses() {
        return classes;
    }
}
