package com.tlc.analytics.config.model.view.layout;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.config.model.view.ViewType;
import com.tlc.analytics.config.model.view.report.ReportType;
import com.tlc.analytics.config.keys.view.layout.DashboardParentKeys;
import com.tlc.analytics.sql.resource.ANADASHBOARDPARENTVIEW;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Table;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the dashboard parent view details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANADASHBOARDPARENTVIEW.TABLE )
public class DashboardParentView extends BaseModel
{
    @JsonKey( key = DashboardParentKeys.DASH_ID )
    @Column( name = ANADASHBOARDPARENTVIEW.DASH_ID )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.dashboard.parent.invalid.dashboard.id" )
    private Long dashId;

    @JsonKey( key = DashboardParentKeys.ROW_INDEX )
    @Column( name = ANADASHBOARDPARENTVIEW.ROW_INDEX )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.dashboard.parent.invalid.row.index" )
    @Max( 10 )
    private Long rowIdx;

    @JsonKey( key = DashboardParentKeys.COLUMN_INDEX )
    @Column( name = ANADASHBOARDPARENTVIEW.COL_INDEX )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.dashboard.parent.invalid.column.index" )
    @Max( 4 )
    private Long colIdx;

    @JsonKey( key = DashboardParentKeys.TITLE )
    @Column( name = ANADASHBOARDPARENTVIEW.TITLE )
    @NotBlank( groups = { Group.Create.class },
               message = "i18n.analytics.dashboard.parent.invalid.title" )
    @Size( max = 20 )
    private String title;

    @JsonKey( key = DashboardParentKeys.VIEW_ID )
    @Column( name = ANADASHBOARDPARENTVIEW.VIEW_ID )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.dashboard.parent.invalid.view.id" )
    private Long childViewId;

    @JsonKey( key = DashboardParentKeys.TYPE )
    @Column( name = ANADASHBOARDPARENTVIEW.VIEW_TYPE )
    @Getter( AccessLevel.NONE )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.dashboard.parent.invalid.report.type" )
    private Integer reportType;

    @JsonKey( key = DashboardParentKeys.WIDTH )
    @Column( name = ANADASHBOARDPARENTVIEW.WIDTH )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.dashboard.parent.invalid.width" )
    @Max( 100 )
    private Long width;

    @JsonKey( key = DashboardParentKeys.HEIGHT )
    @Column( name = ANADASHBOARDPARENTVIEW.HEIGHT )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.dashboard.parent.invalid.height" )
    @Max( 100 )
    private Long height;

    public ReportType getType()
    {
        return ReportType.getReportType( reportType );
    }

    public ViewType getViewType()
    {
        return ViewType.getType( reportType );
    }
}
