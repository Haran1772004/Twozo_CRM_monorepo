package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view;

import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.DashboardLayoutPosition;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.CRMRViewFieldMapper;
import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMCHILDVIEWDETAILS;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Relation;
import com.tlc.entity.annotation.Cascade;
import com.tlc.entity.annotation.Table;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Collection;

/**
 * <p>
 * A model that represents the {@link CRMChildViewDetails}
 * </p>
 *
 * @author jamesh
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name="CRMChildViewDetails" )
public class CRMChildViewDetails extends CRMBaseModel
{
    @Column( name="CRM_DASHBOARD_ID" )
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.dashboard.invalid.id" )
    private Long crmDashboardId;

    @Column( name="CRM_VIEW_POSITION" )
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.invalid.view.position" )
    private Long crmViewPosition;

    @Column( name = CRMCHILDVIEWDETAILS.CRM_VIEW_TYPE )
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.invalid.view.type.id" )
    private Integer viewTypeId;

    @Column( name = "CRM_VIEW_NAME" )
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.invalid.view.name" )
    private String name;

    @Column( name = "CRM_VIEW_DESCRIPTION" )

    private String description;

    @Column( name="CREATED_TIME" )
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.invalid.created.time" )
    private Long createdTime;

    @Column( name="MODIFIED_TIME" )
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.invalid.modified.time" )
    private Long modifiedTime;

    @Column( name="CREATED_BY" )
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.invalid.created.by" )
    private Long createdBy;

    @Column( name="MODIFIED_BY" )
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.invalid.modified.by" )
    private Long modifiedBy;

    @Join( relation = Relation.ONE_TO_MANY,
           cascades = Cascade.ALL,
           nullable = true,
           joinTables = @JoinTable( aliasName = "crmView",
                                    joinColumns = @JoinColumn( referenceColumn = "CRM_VIEW_ID" ),
                                    entityClass = CRMANAViewMapper.class ) )
    private Collection<CRMANAViewMapper> crmViewMapper;

    @Join( relation = Relation.ONE_TO_MANY,
           cascades = Cascade.ALL,
           nullable = true,
           joinTables = @JoinTable( aliasName = "crmViewFields",
                                    joinColumns = @JoinColumn( referenceColumn = "CRM_VIEW_ID" ),
                                    entityClass = CRMRViewFieldMapper.class ) )
    private Collection<CRMRViewFieldMapper> crmViewField;

    @Join(relation = Relation.ONE_TO_ONE,
          nullable = true,
          cascades = Cascade.ALL,
          joinTables = @JoinTable(joinColumns = @JoinColumn(referenceColumn = "VIEW_ID"),
                                  entityClass = DashboardLayoutPosition.class ))
    private DashboardLayoutPosition dashboardLayoutPosition;

    public CRMViewType getViewType()
    {
        return CRMViewType.getCrmViewType( viewTypeId );
    }
}
