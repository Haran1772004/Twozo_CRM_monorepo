package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMTEMPLATEDURATION;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import com.tlc.i18n.I18nKey;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that represents the {@link CRMTemplateDuration}
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table(name = CRMTEMPLATEDURATION.TABLE)
public class CRMTemplateDuration extends CRMBaseModel {
    @Column(name = CRMTEMPLATEDURATION.TEMPLATE_ID)
    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.crm.analytics.template.invalid.id")
    private Long templateId;

    @Column(name = CRMTEMPLATEDURATION.FIELD_ID)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.field.id")
    private Long fieldId;

    @Column(name = CRMTEMPLATEDURATION.TIME_UNIT_ID)
    @NotNull(groups = {Group.Create.class}, message = "i18n.analytics.config.invalid.time.unit")
    private Integer timeUnitId;

    @Column(name = CRMTEMPLATEDURATION.LABEL)
    @NotNull(groups = {Group.Create.class}, message = "i18n.analytics.config.view.invalid.label")
    private I18nKey label;

    @Column(name = CRMTEMPLATEDURATION.FROM)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.from")
    private Long from;

    @Column(name = CRMTEMPLATEDURATION.TO)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.to")
    private Long to;
}
