package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.trash;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMDASHBOARDTRASH;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import lombok.Getter;

import lombok.experimental.SuperBuilder;

/**
 * <p>
 * Represents the {@link CRMDashboardTrash} details
 * </p>
 *
 * @author pandiarajkumar G
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name= CRMDASHBOARDTRASH.TABLE )
public class CRMDashboardTrash extends CRMBaseModel
{
    @Column( name = CRMDASHBOARDTRASH.DASHBOARD_ID )
    private Long dashboardId;

    @Column( name = CRMDASHBOARDTRASH.TRASHED_AT )
    private Long trashedAt;
}
