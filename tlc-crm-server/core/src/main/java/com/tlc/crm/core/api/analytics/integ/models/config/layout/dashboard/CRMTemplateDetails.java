package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMTemplateCompareTo;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMTemplateDuration;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMTemplateMetrics;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMTemplateViewBy;
import com.tlc.crm.core.sql.resource.CRMTEMPLATECOMPARETO;
import com.tlc.crm.core.sql.resource.CRMTEMPLATEDETAILS;
import com.tlc.crm.core.sql.resource.CRMTEMPLATEMETRICS;
import com.tlc.crm.core.sql.resource.CRMTEMPLATEVIEWBY;
import com.tlc.crm.core.sql.resource.CRMTEMPLATEDURATION;

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
 * A model that represents the {@link CRMTemplateDetails}
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table(name = CRMTEMPLATEDETAILS.TABLE)
public class CRMTemplateDetails extends CRMBaseModel {
    @Column(name = CRMTEMPLATEDETAILS.VIEW_NAME)
    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.crm.analytics.template.invalid.name")
    private String viewName;

    @Column(name = CRMTEMPLATEDETAILS.MODULE_ID)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.module.id")
    private Integer moduleId;

    @Column(name = CRMTEMPLATEDETAILS.VIEW_TYPE_ID)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.invalid.view.type.id")
    private Integer viewTypeId;

    @Join(relation = Relation.ONE_TO_MANY,
          cascades = Cascade.ALL,
          nullable = true,
          joinTables = @JoinTable(aliasName = "templateMetrics",
                                  joinColumns = @JoinColumn(referenceColumn = CRMTEMPLATEMETRICS.TEMPLATE_ID),
                                  entityClass = CRMTemplateMetrics.class))
    @Size(max = 4)
    private Collection<CRMTemplateMetrics> templateMetrics;

    @Join(relation = Relation.ONE_TO_MANY,
          cascades = Cascade.ALL,
          nullable = true,
          joinTables = @JoinTable(aliasName = "templateViewBy",
                                  joinColumns = @JoinColumn(referenceColumn = CRMTEMPLATEVIEWBY.TEMPLATE_ID),
                                  entityClass = CRMTemplateViewBy.class))
    @Size(max = 2)
    private Collection<CRMTemplateViewBy> templateViewBy;

    @Join(relation = Relation.ONE_TO_ONE,
          cascades = Cascade.ALL,
          nullable = true,
          joinTables = @JoinTable(aliasName = "templateCompareTo",
                                  joinColumns = @JoinColumn(referenceColumn = CRMTEMPLATECOMPARETO.TEMPLATE_ID),
                                  entityClass = CRMTemplateCompareTo.class))
    private CRMTemplateCompareTo templateCompareTo;

    @Join(relation = Relation.ONE_TO_ONE,
          cascades = Cascade.ALL,
          nullable = true,
          joinTables = @JoinTable(aliasName = "templateDuration",
                                  joinColumns = @JoinColumn(referenceColumn = CRMTEMPLATEDURATION.TEMPLATE_ID),
                                  entityClass = CRMTemplateDuration.class))
    private CRMTemplateDuration templateDuration;

}
