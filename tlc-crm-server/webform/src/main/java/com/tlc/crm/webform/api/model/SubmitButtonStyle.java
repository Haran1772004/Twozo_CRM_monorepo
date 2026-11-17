package com.tlc.crm.webform.api.model;

import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Update;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A Model which represents the webForm submit button style details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class SubmitButtonStyle {

    @NotNull(groups = {Update.class, Delete.class}, message = "i18n.webForm.submitButton.style.invalid.id")
    private Long id;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.submitButton.style.invalid.background.color")
    @Size(max = 7, groups = {Create.class, Update.class}, message = "i18n.webForm.submitButton.style.background.color.max.size")
    private String backgroundColor;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.submitButton.style.invalid.text.color")
    @Size(max = 7, groups = {Create.class, Update.class}, message = "i18n.webForm.submitButton.style.text.color.max.size")
    private String textColor;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.submitButton.style.invalid.alignment")
    private Alignment alignment;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.submitButton.style.invalid.width")
    @Min(value = 25, groups = {Create.class, Update.class}, message = "i18n.webForm.submitButton.style.minimum.width")
    @Max(value = 100, groups = {Create.class, Update.class}, message = "i18n.webForm.submitButton.style.maximum.width")
    private int width;

    public void setId(final Long id) {
        this.id = id;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(final String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(final String textColor) {
        this.textColor = textColor;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public void setAlignment(final Alignment alignment) {
        this.alignment = alignment;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(final int width) {
        this.width = width;
    }

    public Long id() {
        return id;
    }

}
