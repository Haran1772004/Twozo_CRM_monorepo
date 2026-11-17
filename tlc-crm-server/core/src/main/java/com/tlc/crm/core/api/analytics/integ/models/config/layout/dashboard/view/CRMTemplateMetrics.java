package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMTEMPLATEFILTERS;
import com.tlc.crm.core.sql.resource.CRMTEMPLATEMETRICS;
import com.tlc.crm.core.sql.resource.CRMTEMPLATEPERIODICFILTER;

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
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Collection;

/**
 * <p>
 * A model that represents the {@link CRMTemplateMetrics}
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table(name = CRMTEMPLATEMETRICS.TABLE)
public class CRMTemplateMetrics extends CRMBaseModel {
    @Column(name = CRMTEMPLATEMETRICS.TEMPLATE_ID)
    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.crm.analytics.template.invalid.id")
    private Long templateId;

    @Column(name = CRMTEMPLATEMETRICS.MEASURE_ID)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.measure.type")
    private Integer measureId;

    @Column(name = CRMTEMPLATEMETRICS.MODULE_ID)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.module.id")
    private Integer moduleId;

    @Column(name = CRMTEMPLATEMETRICS.FIELD_ID)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.field.id")
    private Long fieldId;

    @Column(name = CRMTEMPLATEMETRICS.LABEL)
    @NotNull(groups = {Group.Create.class}, message = "i18n.analytics.config.view.invalid.label")
    private I18nKey label;

    @Column(name = CRMTEMPLATEMETRICS.ORDER)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.field.invalid.order")
    private int order;

    @Join(relation = Relation.ONE_TO_ONE,
          cascades = Cascade.ALL,
          nullable = true,
          joinTables = @JoinTable(aliasName = "templatePeriodicFilter",
                                  joinColumns = @JoinColumn(referenceColumn = CRMTEMPLATEPERIODICFILTER.METRIC_ID),
                                  entityClass = CRMTemplatePeriodicFilter.class))
    private CRMTemplatePeriodicFilter templatePeriodicFilter;

    @Join(relation = Relation.ONE_TO_MANY,
          cascades = Cascade.ALL,
          nullable = true,
          joinTables = @JoinTable(aliasName = "templateFilters",
                                  joinColumns = @JoinColumn(referenceColumn = CRMTEMPLATEFILTERS.METRIC_ID),
                                  entityClass = CRMTemplateFilters.class))
    @Size(max = 10)
    private Collection<CRMTemplateFilters> templateFilters;
}
