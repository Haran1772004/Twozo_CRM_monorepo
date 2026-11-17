package com.tlc.crm.core.notification.email.template.api.model;

/**
 * <p>
 * provides an abstract way to access names and ids of template placeholder fields
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public interface TemplateFieldHolder {

    /**
     * <p>
     * Retrieves the name of the template placeholder field
     * </p>
     *
     * @return The template placeholder field name
     */
    String getName();
}