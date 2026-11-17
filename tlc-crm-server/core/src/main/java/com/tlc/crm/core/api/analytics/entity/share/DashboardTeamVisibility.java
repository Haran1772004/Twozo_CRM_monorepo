package com.tlc.crm.core.api.analytics.entity.share;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.MCANADTEAMVISBILITY;
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
 * A model that manages the dashboard team visibility details
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table( name = MCANADTEAMVISBILITY.TABLE )
public class DashboardTeamVisibility extends CRMBaseModel
{
    @Column( name = MCANADTEAMVISBILITY.TEAM_ID )
    @NotNull( message = "i18n.crm.analytics.invalid.team.id" )
    private Long teamId;

    @Column( name = MCANADTEAMVISBILITY.PERMISSION_ID )
    @NotNull( message = "i18n.crm.analytics.visibility.permission.invalid.id" )
    private Short visibilityPermissionId;

    @Column( name = MCANADTEAMVISBILITY.DASHBOARD_VISIBILITY_ID )
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
