package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard;

import com.tlc.crm.core.action.service.DashboardAccessControlledModel;
import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.trash.CRMDashboardTrash;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;
import com.tlc.crm.core.api.analytics.permission.DashboardPermissionService;
import com.tlc.crm.core.sql.resource.CRMDASHBOARDTRASH;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Relation;
import com.tlc.entity.annotation.Cascade;
import com.tlc.entity.annotation.Table;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * <p>
 * A model that represents the {@link CRMDashboardDetails}
 * </p>
 *
 * @author jamesh
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name="CRMDashboardDetails" )
public class CRMDashboardDetails extends CRMBaseModel implements DashboardAccessControlledModel
{
    private static final DashboardPermissionService DASHBOARD_PERMISSION_SERVICE = DashboardPermissionService.INSTANCE();

    @Column( name="ANA_DASHBOARD_ID" )
    @NotNull( groups = { Group.Create.class, Group.Update.class, Group.Delete.class }, message = "i18n.crm.analytics.dashboard.invalid.id" )
    private Long reportsDashboardId;

    @Column( name="MARK_AS_TRASH" )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.view.details.invalid.move.to.trash" )
    private boolean markAsTrash;

    @Column( name = "CRM_VIEW_NAME" )
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.invalid.view.name" )
    private String name;

    @Column( name = "CRM_VIEW_DESCRIPTION" )
    private String description;

    @Column( name= "CREATED_TIME" )
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.invalid.created.time" )
    private Long createdTime;

    @Column( name= "MODIFIED_TIME" )
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.invalid.modified.time" )
    private Long modifiedTime;

    @Column( name = "CREATED_BY" )
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.invalid.created.by" )
    private Long createdBy;

    @Column( name = "MODIFIED_BY" )
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.invalid.modified.by" )
    private Long modifiedBy;

    @Column( name = "IS_CURATED" )
    private boolean isCurated;

    @Join( relation = Relation.ONE_TO_MANY,
           cascades = Cascade.ALL,
           nullable = true,
           joinTables = @JoinTable( aliasName = "crmChildViews",
                                    joinColumns = @JoinColumn( referenceColumn = "CRM_DASHBOARD_ID" ),
                                    entityClass = CRMChildViewDetails.class ) )
    @Size( max = 10 )
    private Collection<CRMChildViewDetails> crmChildViews;

    @Join( relation = Relation.ONE_TO_ONE,
           cascades = Cascade.ALL,
           nullable = true,
           joinTables = @JoinTable( joinColumns = @JoinColumn( referenceColumn = CRMDASHBOARDTRASH.DASHBOARD_ID ),
                                    entityClass = CRMDashboardTrash.class ) )
    private CRMDashboardTrash crmDashboardTrash;

    private Boolean isEditable;
    private Boolean isDeletable;
    private Boolean isFavourite;
    private Boolean canClone;
    private Boolean canMoveToTrash;
    private Boolean canRestore;

    public Collection<CRMChildViewDetails> getChildViews()
    {
        if(Objects.nonNull( crmChildViews ) )
        {
            return Collections.unmodifiableCollection( crmChildViews );
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isFavourite()
    {
        if ( Objects.isNull( isFavourite ) )
        {
            this.isFavourite = DASHBOARD_PERMISSION_SERVICE.isFavourite( this.orgId(), this.id() );

            return isFavourite;
        }
        return isFavourite;
    }

    /**
     * {@inheritDoc}
     *
     * @return True if the model can restore, false otherwise
     */
    @Override
    public boolean canRestore()
    {
        if ( Objects.isNull( canRestore ) )
        {
            this.canRestore = DASHBOARD_PERMISSION_SERVICE.canRestore( createdBy );

            return canRestore;
        }
        return canRestore;
    }

    /**
     * {@inheritDoc}
     *
     * @return True if the model can move to trash, false otherwise
     */
    @Override
    public boolean canMoveToTrash()
    {
        if ( Objects.isNull( canMoveToTrash ) )
        {
            this.canMoveToTrash = DASHBOARD_PERMISSION_SERVICE.canTrash( createdBy, isCurated );

            return canMoveToTrash;
        }
        return canMoveToTrash;
    }

    /**
     * {@inheritDoc}
     *
     * @return True if the model can clone, false otherwise
     */
    @Override
    public boolean canClone()
    {
        if ( Objects.isNull( canClone ) )
        {
            this.canClone = DASHBOARD_PERMISSION_SERVICE.canClone();

            return canClone;
        }
        return canClone;
    }

    /**
     * {@inheritDoc}
     *
     * @return True if the model can be edited, false otherwise
     */
    @Override
    public boolean isEditable()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return True if the model can be deleted, false otherwise
     */
    @Override
    public boolean isDeletable()
    {
        if ( Objects.isNull( isDeletable ) )
        {
            this.isDeletable = DASHBOARD_PERMISSION_SERVICE.canDelete( createdBy );

            return isDeletable;
        }
        return isDeletable;
    }
}
