package com.tlc.crm.webform.api.model;

import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Update;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * A Model which represents the webForm field style details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class FieldStyle {

    @NotNull(groups = {Update.class, Delete.class}, message = "i18n.webForm.field.style.invalid.id")
    private Long id;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.field.style.invalid.label.color")
    @Size(max = 7, groups = {Create.class, Update.class}, message = "i18n.webForm.field.style.label.color.max.size")
    private String labelColor;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.field.style.invalid.input.text.color")
    @Size(max = 7, groups = {Create.class, Update.class}, message = "i18n.webForm.field.style.input.text.color.max.size")
    private String inputTextColor;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.field.style.invalid.input.background.color")
    @Size(max = 7, groups = {Create.class, Update.class}, message = "i18n.webForm.field.style.input.background.color.max.size")
    private String inputBackgroundColor;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.field.style.invalid.label.font.size")
    @Min(value = 11, groups = {Create.class, Update.class}, message = "i18n.webForm.field.style.label.font.size.minimum")
    @Max(value = 24, groups = {Create.class, Update.class}, message = "i18n.webForm.field.style.label.font.size.maximum")
    private int labelFontSize;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.field.style.invalid.input.font.size")
    @Min(value = 11, groups = {Create.class, Update.class}, message = "i18n.webForm.field.style.input.font.size.minimum")
    @Max(value = 24, groups = {Create.class, Update.class}, message = "i18n.webForm.field.style.input.font.size.maximum")
    private int inputFontSize;

    public void setId(final Long id) {
        this.id = id;
    }

    public String getLabelColor() {
        return labelColor;
    }

    public void setLabelColor(final String labelColor) {
        this.labelColor = labelColor;
    }

    public String getInputTextColor() {
        return inputTextColor;
    }

    public void setInputTextColor(final String inputTextColor) {
        this.inputTextColor = inputTextColor;
    }

    public String getInputBackgroundColor() {
        return inputBackgroundColor;
    }

    public void setInputBackgroundColor(final String inputBackgroundColor) {
        this.inputBackgroundColor = inputBackgroundColor;
    }

    public int getLabelFontSize() {
        return labelFontSize;
    }

    public void setLabelFontSize(final int labelFontSize) {
        this.labelFontSize = labelFontSize;
    }

    public int getInputFontSize() {
        return inputFontSize;
    }

    public void setInputFontSize(final int inputFontSize) {
        this.inputFontSize = inputFontSize;
    }

    public Long id() {
        return id;
    }

}
