package com.tlc.crm.webform.field;

import com.tlc.crm.entity.models.FieldNameHolder;

import java.util.Objects;

/**
 * Represents the fields related to webForm styles.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum WebFormStyleFields implements FieldNameHolder {

    ID("id"),
    STYLE("style"),
    FORM_STYLE("formStyle"),
    FIELD_STYLE("fieldStyle"),
    SUBMIT_BUTTON_STYLE("submitButtonStyle"),
    HEADER("header"),
    SUB_HEADER("subHeader"),
    BACKGROUND_COLOR("backgroundColor"),
    TEXT_COLOR("textColor"),
    FONT_SIZE("fontSize"),
    ALIGNMENT_ID("alignmentId"),
    FONT_ID("fontId"),
    LABEL("label"),
    INPUT("input"),
    COLOR("color"),
    WIDTH("width"),
    DESCRIPTION("description"),
    TEXT("text"),
    ;

    private final String name;

    WebFormStyleFields(final String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public String getName() {
        return name;
    }
}
