package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard;


import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@Table(name = "DashboardLayoutPosition")
public class DashboardLayoutPosition extends CRMBaseModel {

    @Column(name = "DASHBOARD_ID")
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.invalid.dashboard.id" )
    private Long dashboardId;

    @Column(name = "VIEW_ID")
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.invalid.view.id" )
    private Long viewId;

    @Column(name = "POSITION")
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.invalid.position" )
    private  Long position;

    @Column(name = "LAYOUT_CONFIG")
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.invalid.layout.id" )
    private String layoutAsString;

    private DashboardLayoutConfig dashboardLayoutConfig;
}
