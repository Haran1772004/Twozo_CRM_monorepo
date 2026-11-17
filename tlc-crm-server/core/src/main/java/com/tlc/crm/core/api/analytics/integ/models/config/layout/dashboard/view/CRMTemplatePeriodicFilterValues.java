package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMTEMPLATEPERIODICFILTERVALUES;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that represents the {@link CRMTemplatePeriodicFilterValues}
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table(name = CRMTEMPLATEPERIODICFILTERVALUES.TABLE)
public class CRMTemplatePeriodicFilterValues extends CRMBaseModel {
    @Column(name = CRMTEMPLATEPERIODICFILTERVALUES.TEMPLATE_PERIODIC_FILTER_ID)
    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.crm.analytics.invalid.periodic.filter.id")
    private Long periodicFilterId;

    @Column(name = CRMTEMPLATEPERIODICFILTERVALUES.VALUE)
    @NotNull(groups = {Group.Create.class}, message = "i18n.analytics.config.invalid.time.unit")
    private Long timeUnitValue;

    @Column(name = CRMTEMPLATEPERIODICFILTERVALUES.FROM)
    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.crm.analytics.invalid.from")
    private Long from;

    @Column(name = CRMTEMPLATEPERIODICFILTERVALUES.TO)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.to")
    private Long to;
}
