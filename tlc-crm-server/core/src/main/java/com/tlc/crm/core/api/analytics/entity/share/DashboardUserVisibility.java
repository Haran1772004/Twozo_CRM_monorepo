package com.tlc.crm.core.api.analytics.entity.share;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.MCANADUSERVISBILITY;
import com.tlc.crm.core.sql.resource.OMCANADASHBOARDVISBILITY;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.entity.annotation.Relation;
import com.tlc.entity.annotation.Table;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the dashboard user visibility details
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table( name = MCANADUSERVISBILITY.TABLE )
public class DashboardUserVisibility extends CRMBaseModel
{
    @Column( name = MCANADUSERVISBILITY.USER_ID )
    @NotNull( message = "i18n.crm.analytics.invalid.user.id" )
    private Long userId;

    @Column( name = MCANADUSERVISBILITY.PERMISSION_ID )
    @NotNull( message = "i18n.crm.analytics.visibility.permission.invalid.id" )
    private Short visibilityPermissionId;

    @Column( name = MCANADUSERVISBILITY.DASHBOARD_VISIBILITY_ID )
    @NotNull( message = "i18n.crm.analytics.dashboard.visibility.invalid.id" )
    @Join( relation   = Relation.ONE_TO_MANY,
           joinTables = @JoinTable( joinColumns = @JoinColumn( referenceColumn = OMCANADASHBOARDVISBILITY.ID ),
                                    entityClass = DashboardVisibility.class ) )
    private Long dashboardVisibilityId;

    public VisibilityPermission getVisibilityPermission()
    {
        return VisibilityPermission.get( visibilityPermissionId );
    }
}
