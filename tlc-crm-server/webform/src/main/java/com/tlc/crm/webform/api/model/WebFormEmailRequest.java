package com.tlc.crm.webform.api.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.webform.internal.status.WebFormErrorCodes;
import com.tlc.notification.model.alert.EmailNotifiableModel;
import com.tlc.validator.TlcModel;

/**
 * Represents the model for web form email notifications
 *
 * @author kishore J
 * @version 1.0
 */
public class WebFormEmailRequest implements EmailNotifiableModel, TlcModel {

    private WebForm webForm;
    private Entity entity;
    private String domain;

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(final Entity entity) {
        this.entity = entity;
    }

    public WebForm getWebForm() {
        return webForm;
    }

    public void setWebForm(final WebForm webForm) {
        this.webForm = webForm;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(final String domain) {
        this.domain = domain;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDateTimeField() {
        return true;
    }

    @Override
    public Long id() {
        return webForm.id();
    }

    @Override
    public Long orgId() {
        return webForm.orgId();
    }

    @Override
    public Object identity() {
        throw ErrorCode.get(WebFormErrorCodes.UNSUPPORTED_OPERATION);
    }
}