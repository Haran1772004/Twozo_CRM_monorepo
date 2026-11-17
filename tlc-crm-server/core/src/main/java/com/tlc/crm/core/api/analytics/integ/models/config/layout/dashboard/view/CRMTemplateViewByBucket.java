package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMTEMPLATEVIEWBYBUCKET;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that represents the {@link CRMTemplateViewByBucket}
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table(name = CRMTEMPLATEVIEWBYBUCKET.TABLE)
public class CRMTemplateViewByBucket extends CRMBaseModel {
    @Column(name = CRMTEMPLATEVIEWBYBUCKET.TEMPLATE_VIEW_BY_ID)
    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.crm.analytics.dashboard.invalid.id")
    private Long viewById;

    @Column(name = CRMTEMPLATEVIEWBYBUCKET.UNIT_ID)
    @NotNull(groups = {Group.Create.class}, message = "i18n.analytics.config.invalid.time.unit")
    private Long unitId;

    @Column(name = CRMTEMPLATEVIEWBYBUCKET.FROM)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.from")
    private Long from;

    @Column(name = CRMTEMPLATEVIEWBYBUCKET.TO)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.to")
    private Long to;
}
