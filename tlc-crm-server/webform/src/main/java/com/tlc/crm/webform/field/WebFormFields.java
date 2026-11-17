package com.tlc.crm.webform.field;

import com.tlc.crm.entity.models.FieldNameHolder;

import java.util.Objects;

/**
 * Represents the fields related to web form.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum WebFormFields implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    NAME("name"),
    FORM_NAME("formName"),
    DESCRIPTION("description"),
    SUBMIT_BUTTON_NAME("submitButtonName"),
    IS_ACTIVE("isActive"),
    OWNER_ID("ownerId"),
    WEB_FORMS("webForms"),
    DATA("data"),
    TEMPLATE_ID("templateId"),
    LANGUAGE_ID("languageId"),
    VISITS("visits"),
    SUBMISSIONS("submissions"),
    CONVERSIONS("conversions"),
    CREATED_TIME("createdTime"),
    UPDATED_TIME("updatedTime"),
    FORM_PROPERTIES("formProperties"),
    STATUS("status"),
    LABEL("label"),
    THANK_YOU_MESSAGE("thankYouMessage"),
    REDIRECT_URL("redirectUrl"),
    IS_ENABLED("isEnabled"),
    RECIPIENTS("recipients"),
    VALUE("value"),
    ON_SUBMIT("onSubmit"),
    ATTACHMENT_ID("attachmentId"),
    TYPE("type"),
    IS_REQUIRED("isRequired"),
    ORDER("order"),
    FIELD_ID("fieldId"),
    EMAILS("emails"),
    PHONES("phones"),
    DISPLAY_NAME("displayName"),
    FIELD_TYPE("fieldType"),
    TEMPLATES("templates"),
    CONFIG("config"),
    START("start"),
    LIMIT("limit"),
    TOTAL_RECORDS("totalRecords"),
    HAS_MORE("hasMore"),
    EMAIL_NOTIFICATION("emailNotification"),
    IS_CONTACT_CREATION_ENABLED("isContactCreationEnabled"),
    IS_DEAL_CREATION_WITH_CONTACT_ENABLED("isDealCreationWithContactEnabled"),
    WEBSITE("website"),
    CURRENCY_ID("currencyId"),
    FIELD_TYPE_ID("fieldTypeId"),
    ERRORS("errors"),
    SORT("sort"),
    FIELD("field"),
    ASC("asc"),
    DESC("desc"),
    MESSAGE("message"),
    URL("url"),
    OPTIONS("options"),
    SIZE("size"),
    OPTION("option"),
    FIRST_NAME("firstName"),
    LAST_NAME("lastName"),
    COMPANY("company"),
    DUPLICATED_WEB_FORM_ID("duplicatedWebFormId"),
    IFRAME_URL("iframeUrl"),
    IS_IFRAME("isIframe"),
    ERROR("error"),
    IS_SYSTEM_FIELD("isSystemField"),
    REF_ID("refId"),
    ADDRESS("address"),
    EXTENSION("extension"),
    ORIGINAL("original"),
    THUMBNAIL("thumbnail"),
    ATTACHMENT_INFO("attachmentInfo"),
    HEIGHT("height"),
    WIDTH("width"),
    ALIGN("align"),
    SIZE_TYPE_ID("sizeTypeId"),
    ;

    private final String name;

    WebFormFields(final String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public String getName() {
        return name;
    }
}
