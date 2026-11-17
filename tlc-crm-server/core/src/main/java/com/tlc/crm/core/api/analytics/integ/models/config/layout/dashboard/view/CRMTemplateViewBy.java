package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMTEMPLATEVIEWBY;
import com.tlc.crm.core.sql.resource.CRMTEMPLATEVIEWBYBUCKET;

import com.tlc.entity.annotation.Table;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.Relation;
import com.tlc.entity.annotation.Cascade;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.JoinTable;

import com.tlc.i18n.I18nKey;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that represents the {@link CRMTemplateViewBy}
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table(name = CRMTEMPLATEVIEWBY.TABLE)
public class CRMTemplateViewBy extends CRMBaseModel {
    @Column(name = CRMTEMPLATEVIEWBY.TEMPLATE_ID)
    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.crm.analytics.template.invalid.id")
    private Long templateId;

    @Column(name = CRMTEMPLATEVIEWBY.FIELD_ID)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.field.id")
    private Long fieldId;

    @Column(name = CRMTEMPLATEVIEWBY.MODULE_ID)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.module.id")
    private Integer moduleId;

    @Column(name = CRMTEMPLATEVIEWBY.SUB_MODULE_ID)
    private Integer subModuleId;

    @Column(name = CRMTEMPLATEVIEWBY.TIME_PERIOD_FIELD_ID)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.time.period.field.id")
    private Long timePeriodFieldId;

    @Column(name = CRMTEMPLATEVIEWBY.LABEL)
    @NotNull(groups = {Group.Create.class}, message = "i18n.analytics.config.view.invalid.label")
    private I18nKey label;

    @Column(name = CRMTEMPLATEVIEWBY.ORDER)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.field.invalid.order")
    private int order;

    @Join(relation = Relation.ONE_TO_ONE,
          cascades = Cascade.ALL,
          nullable = true,
          joinTables = @JoinTable(aliasName = "templateViewByBucket",
                                  joinColumns = @JoinColumn(referenceColumn = CRMTEMPLATEVIEWBYBUCKET.TEMPLATE_VIEW_BY_ID),
                                  entityClass = CRMTemplateViewByBucket.class))
    private CRMTemplateViewByBucket templateViewByBucket;

}
