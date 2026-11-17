package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMTEMPLATEPERIODICFILTER;
import com.tlc.crm.core.sql.resource.CRMTEMPLATEPERIODICFILTERVALUES;

import com.tlc.entity.annotation.Table;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.Relation;
import com.tlc.entity.annotation.Cascade;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.JoinTable;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that represents the {@link CRMTemplatePeriodicFilter}
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table(name = CRMTEMPLATEPERIODICFILTER.TABLE)
public class CRMTemplatePeriodicFilter extends CRMBaseModel {
    @Column(name = CRMTEMPLATEPERIODICFILTER.METRIC_ID)
    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.crm.analytics.invalid.metric.id")
    private Long metricId;

    @Column(name = CRMTEMPLATEPERIODICFILTER.COMPARATOR_ID)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.comparator")
    private Integer comparatorId;

    @Column(name = CRMTEMPLATEPERIODICFILTER.FIELD_ID)
    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.crm.analytics.invalid.field.id")
    private Long fieldId;

    @Column(name = CRMTEMPLATEPERIODICFILTER.FUNCTION_ID)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.function.id")
    private Integer functionId;

    @Join(relation = Relation.ONE_TO_ONE,
          cascades = Cascade.ALL,
          nullable = true,
          joinTables = @JoinTable(aliasName = "templatePeriodicFilterValues",
                                  joinColumns = @JoinColumn(referenceColumn = CRMTEMPLATEPERIODICFILTERVALUES.TEMPLATE_PERIODIC_FILTER_ID),
                                  entityClass = CRMTemplatePeriodicFilterValues.class))
    private CRMTemplatePeriodicFilterValues templatePeriodicFilterValues;

}
