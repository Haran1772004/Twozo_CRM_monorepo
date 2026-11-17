package com.tlc.crm.core.notification.email.template.fields;

import com.tlc.crm.entity.models.FieldNameHolder;

import com.tlc.crm.core.notification.email.template.api.model.TemplateFolder;

/**
 * <p>
 * Represents fields related to {@link TemplateFolder}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public enum TemplateFolderFields implements FieldNameHolder {

    ID("id"),
    NAME("name"),
    ORG_ID("orgId"),
    CREATED_TIME("createdTime"),
    UPDATED_TIME("updatedTime"),
    ACCESS_TYPE("accessType"),
    TEMPLATES("templates"),
    TARGET_FOLDER("targetFolder"),
    DATA("data"),
    TYPE("type"),
    USERS("users"),
    TERRITORIES("territories"),
    TEAMS("teams"),
    VISIBLE_TO("visibleTo"),
    OWNER_ID("ownerId"),
    PREVENT_SHARE("preventShare"),
    HAS_OWNER_ICON("hasOwnerIcon"),
    OWNER("owner"),
    ERROR("error"),
    HAS_MORE("hasMore"),
    TOTAL_RECORDS("totalRecords"),
    UPDATED_RECORDS("updatedRecords"),
    IS_EDITABLE("isEditable"),
    IS_VISIBILITY_EDITABLE("isVisibilityEditable"),
    TEMPLATES_COUNT("templatesCount"),
    IS_DEFAULT("isDefault")
    ;

    private final String fieldName;

    TemplateFolderFields(final String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String getName() {
        return this.fieldName;
    }
}
