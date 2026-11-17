package com.tlc.crm.webform.api.model;

import com.tlc.validator.type.Group.Update;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Delete;
import jakarta.validation.constraints.NotNull;

/**
 * A Model which represents the webFom email details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class RecipientEmail {

    @NotNull(groups = Delete.class, message = "i18n.webForm.recipient.email.invalid.id")
    private Long id;

    @com.tlc.validator.type.Email(groups = {Create.class, Update.class}, message = "i18n.webForm.recipient.email.value.invalid")
    private String value;

    public Long id() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }
}
