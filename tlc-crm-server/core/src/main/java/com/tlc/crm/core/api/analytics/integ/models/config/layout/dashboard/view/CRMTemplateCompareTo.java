package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMTEMPLATECOMPARETO;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that represents the {@link CRMTemplateCompareTo}
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table(name = CRMTEMPLATECOMPARETO.TABLE)
public class CRMTemplateCompareTo extends CRMBaseModel {
    @Column(name = CRMTEMPLATECOMPARETO.TEMPLATE_ID)
    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.crm.analytics.template.invalid.id")
    private Long templateId;

    @Column(name = CRMTEMPLATECOMPARETO.COMPARATOR_ID)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.comparator")
    private Long comparatorId;

    @Column(name = CRMTEMPLATECOMPARETO.FROM)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.from")
    private Long from;

    @Column(name = CRMTEMPLATECOMPARETO.TO)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.to")
    private Long to;
}
