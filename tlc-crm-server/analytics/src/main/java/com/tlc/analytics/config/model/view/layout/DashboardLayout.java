package com.tlc.analytics.config.model.view.layout;

import com.tlc.analytics.sql.resource.ANADASHBOARDLAYOUT;
import com.tlc.analytics.sql.resource.ANADASHBOARDPARENTVIEW;
import com.tlc.analytics.config.keys.view.layout.DashboardKeys;

import com.tlc.entity.annotation.Cascade;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Relation;
import com.tlc.entity.annotation.Table;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.entity.annotation.JoinColumn;

import jakarta.validation.constraints.Size;

import lombok.experimental.SuperBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;


/**
 * <p>
 * A model that manages the dashboard layout details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@SuperBuilder
@Table( name = ANADASHBOARDLAYOUT.TABLE )
public class DashboardLayout extends BaseViewLayout
{
    @JsonKey( key = DashboardKeys.CHILD_VIEWS )
    @Join( relation = Relation.ONE_TO_MANY, cascades = Cascade.ALL,
           nullable = true,
           joinTables = @JoinTable( aliasName = "parentView",
                                    joinColumns = @JoinColumn( referenceColumn = ANADASHBOARDPARENTVIEW.DASH_ID ),
                                    entityClass = DashboardParentView.class ) )
    @Size( max = 30 )
    private Collection<DashboardParentView> parentViews;

    public Collection<DashboardParentView> getParentView()
    {
        if( Objects.nonNull( parentViews ) )
        {
            return Collections.unmodifiableCollection( parentViews );
        }
        return Collections.emptyList();
    }
}
