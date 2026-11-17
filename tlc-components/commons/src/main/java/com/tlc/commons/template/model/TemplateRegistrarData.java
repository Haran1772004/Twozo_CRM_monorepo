package com.tlc.commons.template.model;

import com.tlc.commons.service.registrar.model.RegistrarData;

import java.util.Collection;

/**
 * <p>
 * A model that holds the data of the {@link TemplateRegistrarData} which is needed for registering the resources actions
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class TemplateRegistrarData extends RegistrarData {

    private Collection<String> templateUrls;

    public void setTemplateUrls(final Collection<String> templateUrls) {
        this.templateUrls = templateUrls;
    }

    public Collection<String> getTemplateUrls() {
        return templateUrls;
    }
}