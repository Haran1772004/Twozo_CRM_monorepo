package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMTEMPLATEFILTERS;
import com.tlc.crm.core.sql.resource.CRMTEMPLATEFILTERVALUES;

import com.tlc.entity.annotation.Table;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.Relation;
import com.tlc.entity.annotation.Cascade;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.JoinTable;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Collection;


/**
 * <p>
 * A model that represents the {@link CRMTemplateFilters}
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table(name = CRMTEMPLATEFILTERS.TABLE)
public class CRMTemplateFilters extends CRMBaseModel {
    @Column(name = CRMTEMPLATEFILTERS.METRIC_ID)
    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.crm.analytics.invalid.metric.id")
    private Long metricId;

    @Column(name = CRMTEMPLATEFILTERS.COMPARATOR_ID)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.comparator")
    private Integer comparatorId;

    @Column(name = CRMTEMPLATEFILTERS.FIELD_ID )
    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.crm.analytics.invalid.field.id")
    private Long fieldId;

    @Column(name = CRMTEMPLATEFILTERS.FUNCTION_ID )
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.function.id")
    private Integer functionId;

    @Column(name = CRMTEMPLATEFILTERS.ORDER)
    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.crm.analytics.field.invalid.order")
    private int order;

    @Join(relation = Relation.ONE_TO_MANY,
          cascades = Cascade.ALL,
          nullable = true,
          joinTables = @JoinTable(aliasName = "templateFilterValues",
                                  joinColumns = @JoinColumn(referenceColumn = CRMTEMPLATEFILTERVALUES.TEMPLATE_FILTER_ID),
                                  entityClass = CRMTemplateFilterValues.class))
    @Size(max = 10)
    private Collection<CRMTemplateFilterValues> templateFilterValues;
}
