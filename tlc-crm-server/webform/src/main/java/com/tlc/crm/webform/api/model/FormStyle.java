package com.tlc.crm.webform.api.model;

import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Update;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * A Model which represents the Form style details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class FormStyle {

    @NotNull(groups = {Update.class, Delete.class}, message = "i18n.webForm.form.style.invalid.id")
    private Long id;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.invalid.background.color")
    @Size(max = 7, groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.background.color.max.size")
    private String backgroundColor;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.invalid.header.text.color")
    @Size(max = 7, groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.header.text.color.max.size")
    private String headerTextColor;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.invalid.subHeader.text.color")
    @Size(max = 7, groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.subHeader.text.color.max.size")
    private String subHeaderTextColor;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.invalid.header.font.size")
    @Min(value = 11, groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.header.font.size.minimum")
    @Max(value = 24, groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.header.font.size.maximum")
    private int headerFontSize;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.invalid.subHeader.font.size")
    @Min(value = 11, groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.subHeader.font.size.minimum")
    @Max(value = 24, groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.subHeader.font.size.maximum")
    private int subHeaderFontSize;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.invalid.alignment")
    private Alignment alignment;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.invalid.font")
    private Font font;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.invalid.description.text.color")
    @Size(max = 7, groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.description.text.color.max.size")
    private String descriptionTextColor;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.invalid.text.color")
    @Size(max = 7, groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.text.color.max.size")
    private String textColor;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.invalid.description.font.size")
    @Min(value = 11, groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.description.font.size.minimum")
    @Max(value = 24, groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.description.font.size.maximum")
    private int descriptionFontSize;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.invalid.text.font.size")
    @Min(value = 11, groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.text.font.size.minimum")
    @Max(value = 24, groups = {Create.class, Update.class}, message = "i18n.webForm.form.style.text.font.size.maximum")
    private int textFontSize;

    public void setId(final Long id) {
        this.id = id;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(final String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getHeaderTextColor() {
        return headerTextColor;
    }

    public void setHeaderTextColor(final String headerTextColor) {
        this.headerTextColor = headerTextColor;
    }

    public String getSubHeaderTextColor() {
        return subHeaderTextColor;
    }

    public void setSubHeaderTextColor(final String subHeaderTextColor) {
        this.subHeaderTextColor = subHeaderTextColor;
    }

    public int getHeaderFontSize() {
        return headerFontSize;
    }

    public void setHeaderFontSize(final int headerFontSize) {
        this.headerFontSize = headerFontSize;
    }

    public int getSubHeaderFontSize() {
        return subHeaderFontSize;
    }

    public void setSubHeaderFontSize(final int subHeaderFontSize) {
        this.subHeaderFontSize = subHeaderFontSize;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public void setAlignment(final Alignment alignment) {
        this.alignment = alignment;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(final Font font) {
        this.font = font;
    }

    public Long id() {
        return id;
    }

    public String getDescriptionTextColor() {
        return descriptionTextColor;
    }

    public void setDescriptionTextColor(final String descriptionTextColor) {
        this.descriptionTextColor = descriptionTextColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(final String textColor) {
        this.textColor = textColor;
    }

    public int getDescriptionFontSize() {
        return descriptionFontSize;
    }

    public void setDescriptionFontSize(final int descriptionFontSize) {
        this.descriptionFontSize = descriptionFontSize;
    }

    public int getTextFontSize() {
        return textFontSize;
    }

    public void setTextFontSize(final int textFontSize) {
        this.textFontSize = textFontSize;
    }
}
