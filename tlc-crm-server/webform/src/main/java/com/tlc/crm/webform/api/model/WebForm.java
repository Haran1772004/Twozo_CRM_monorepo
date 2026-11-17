package com.tlc.crm.webform.api.model;

import com.tlc.crm.common.models.Language;
import com.tlc.crm.webform.url.UrlGenerator;
import com.tlc.crm.webform.validator.annotation.WebFormValidator;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Update;
import com.tlc.validator.type.Group.Delete;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * A Model which represents the WebForm details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@WebFormValidator(groups = {Create.class, Update.class}, message = "i18n.webForm.validation.failed")
public final class WebForm implements TlcModel {

    @NotNull(groups = {Update.class, Delete.class}, message = "i18n.webForm.invalid.id")
    private Long id;

    @NotNull(groups = {Create.class, Update.class, Delete.class}, message = "i18n.webForm.invalid.orgId")
    private Long orgId;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.invalid.name")
    @Size(max = 100, groups = {Create.class, Update.class}, message = "i18n.webForm.invalid.name.max.length")
    private String name;

    @Size(max = 150, groups = {Create.class, Update.class}, message = "i18n.webForm.invalid.description.max.length")
    private String description;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.invalid.submit.button.name")
    private String submitButtonName;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.invalid.owner.id")
    private Long ownerId;

    @Valid
    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.invalid.form.fieldProperty")
    private Collection<FormFieldProperty> formFieldProperties;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.invalid.template.id")
    private Long templateId;

    private boolean isActive;

    @Valid
    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.invalid.form.style")
    private FormStyle formStyle;

    @Valid
    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.invalid.field.style")
    private FieldStyle fieldStyle;

    @Valid
    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.invalid.submit.button.style")
    private SubmitButtonStyle submitButtonStyle;

    @Valid
    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.invalid.submitAction")
    private SubmitAction submitAction;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.webForm.invalid.language")
    private Language language;

    private Long attachmentId;
    private Long styleId;
    private Long submitActionId;
    private long visits;
    private long submissions;
    private double conversions;
    private Long createdTime;
    private Long updatedTime;

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(final Language language) {
        this.language = language;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Collection<FormFieldProperty> getFormFieldProperties() {
        return formFieldProperties;
    }

    public void setFormFieldProperties(final Collection<FormFieldProperty> formFieldProperties) {
        this.formFieldProperties = formFieldProperties;
    }

    public void addFormFieldProperty(final FormFieldProperty formFieldProperty) {
        this.formFieldProperties = Objects.isNull(this.formFieldProperties) ? new ArrayList<>() : this.formFieldProperties;

        this.formFieldProperties.add(formFieldProperty);
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(final Long templateId) {
        this.templateId = templateId;
    }

    public String getSubmitButtonName() {
        return submitButtonName;
    }

    public void setSubmitButtonName(final String submitButtonName) {
        this.submitButtonName = submitButtonName;
    }

    public SubmitAction getSubmitAction() {
        return submitAction;
    }

    public void setSubmitAction(final SubmitAction submitAction) {
        this.submitAction = submitAction;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(final Long ownerId) {
        this.ownerId = ownerId;
    }

    public Long getSubmitActionId() {
        return submitActionId;
    }

    public void setSubmitActionId(final Long submitActionId) {
        this.submitActionId = submitActionId;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(final boolean active) {
        isActive = active;
    }

    public FormStyle getFormStyle() {
        return formStyle;
    }

    public void setFormStyle(final FormStyle formStyle) {
        this.formStyle = formStyle;
    }

    public FieldStyle getFieldStyle() {
        return fieldStyle;
    }

    public void setFieldStyle(final FieldStyle fieldStyle) {
        this.fieldStyle = fieldStyle;
    }

    public SubmitButtonStyle getSubmitButtonStyle() {
        return submitButtonStyle;
    }

    public void setSubmitButtonStyle(final SubmitButtonStyle submitButtonStyle) {
        this.submitButtonStyle = submitButtonStyle;
    }

    public Long getStyleId() {
        return styleId;
    }

    public void setStyleId(Long styleId) {
        this.styleId = styleId;
    }

    public long getVisits() {
        return visits;
    }

    public void setVisits(final long visits) {
        this.visits = visits;
    }

    public long getSubmissions() {
        return submissions;
    }

    public void setSubmissions(final long submissions) {
        this.submissions = submissions;
    }

    public double getConversions() {
        return conversions;
    }

    public void setConversions(final double conversions) {
        this.conversions = conversions;
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final Long createdTime) {
        this.createdTime = createdTime;
    }

    public Long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(final Long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Long getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(final Long attachmentId) {
        this.attachmentId = attachmentId;
    }

    public String getUrl() {
        return UrlGenerator.get().getUrl(orgId, ownerId, id);
    }

    public double getConversionValue(final boolean isFormVisited) {
        if (isFormVisited) {
            visits += 1;
        } else {
            submissions += 1;
        }

        return Math.round(((submissions * 1.0 / visits) * 100) * 100.0) / 100.0;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }
}
