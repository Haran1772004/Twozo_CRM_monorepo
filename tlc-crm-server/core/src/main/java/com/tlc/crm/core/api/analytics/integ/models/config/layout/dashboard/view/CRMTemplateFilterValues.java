package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.crm.core.sql.resource.CRMTEMPLATEFILTERVALUES;

import com.tlc.entity.annotation.Table;
import com.tlc.entity.annotation.Column;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that represents the {@link CRMTemplateFilterValues}
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table(name = CRMTEMPLATEFILTERVALUES.TABLE)
public class CRMTemplateFilterValues extends BaseModel {
    @Column(name = CRMTEMPLATEFILTERVALUES.TEMPLATE_FILTER_ID)
    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.crm.analytics.invalid.custom.filter.id")
    private Long filterId;

    @Column(name = CRMTEMPLATEFILTERVALUES.VALUE)
    @NotNull(groups = {Group.Create.class}, message = "i18n.analytics.config.invalid.time.unit")
    private Long timeUnitValue;

    @Column(name = CRMTEMPLATEFILTERVALUES.FROM)
    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.crm.analytics.invalid.from")
    private Long from;

    @Column(name = CRMTEMPLATEFILTERVALUES.TO)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.to")
    private Long to;

}