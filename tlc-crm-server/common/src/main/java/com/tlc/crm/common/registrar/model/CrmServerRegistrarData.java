package com.tlc.crm.common.registrar.model;

import com.tlc.commons.service.registrar.model.RegistrarData;
import com.tlc.crm.common.registrar.CommonResourceRegistrar;

import java.util.Collection;

/**
 * <p>
 * A model that holds the data of the {@link CommonResourceRegistrar} which is needed for registering the resources/actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CrmServerRegistrarData extends RegistrarData {

    private Collection<Class<?>> classes;

    public void setClasses(final Collection<Class<?>> classes) {
        this.classes = classes;
    }

    public Collection<Class<?>> getClasses() {
        return classes;
    }
}
