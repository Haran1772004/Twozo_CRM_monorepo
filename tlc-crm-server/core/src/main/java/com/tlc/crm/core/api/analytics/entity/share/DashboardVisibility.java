package com.tlc.crm.core.api.analytics.entity.share;

import com.tlc.analytics.config.model.view.layout.DashboardLayout;
import com.tlc.analytics.sql.resource.ANADASHBOARDLAYOUT;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
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
 * A model that manages the dashboard visibility details
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table( name = OMCANADASHBOARDVISBILITY.TABLE )
public class DashboardVisibility extends CRMBaseModel
{
    @Column( name = OMCANADASHBOARDVISBILITY.DASH_ID )
    @NotNull( message = "i18n.crm.analytics.invalid.dashboard.id" )
    @Join( relation = Relation.ONE_TO_MANY,
           joinTables = @JoinTable( joinColumns = @JoinColumn( referenceColumn = ANADASHBOARDLAYOUT.ID ),
                                    entityClass = DashboardLayout.class ) )
    private Long dashId;

    @Column( name = OMCANADASHBOARDVISBILITY.VISIBILITY_ID )
    @NotNull( message = "i18n.crm.analytics.dashboard.visibility.invalid.id" )
    private Short visibilityId;

    @Column( name = OMCANADASHBOARDVISBILITY.MODIFIED_BY )
    @NotNull( message = "i18n.crm.analytics.invalid.modified.by" )
    private Long modifiedBy;

    @Column( name = OMCANADASHBOARDVISBILITY.CREATED_BY )
    @NotNull( message = "i18n.crm.analytics.invalid.created.by" )
    private Long createdBy;

    public Visibility getVisibility()
    {
        return Visibility.get( visibilityId );
    }
}
