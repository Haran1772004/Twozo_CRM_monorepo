package com.tlc.web.client.registrar.model;

import com.tlc.commons.service.registrar.model.RegistrarData;
import com.tlc.web.client.registrar.WebClientResourceRegistrar;

import java.util.Collection;

/**
 * <p>
 * A model that holds the data of the {@link WebClientResourceRegistrar} which is needed for registering the resources/actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class WebClientRegistrarData extends RegistrarData {

    private Collection<Class<?>> actionClasses;

    public void setActionClasses(final Collection<Class<?>> actionClasses) {
        this.actionClasses = actionClasses;
    }

    public Collection<Class<?>> getActionClasses() {
        return actionClasses;
    }
}
