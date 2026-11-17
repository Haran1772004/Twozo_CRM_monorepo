package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view;

import com.tlc.analytics.config.keys.UserKeys;
import com.tlc.analytics.config.keys.view.favourite.FavouriteKeys;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMFAVOURITEVIEW;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Table;

import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the favourite details
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = CRMFAVOURITEVIEW.TABLE )
public class CRMFavourite extends CRMBaseModel
{
    @JsonKey( key = UserKeys.USER_ID )
    @Column( name = UserKeys.USER_ID_COLUMN )
    @NotNull( groups = { Group.Create.class, Group.Update.class, Group.Delete.class },
              message = "i18n.analytics.favourite.invalid.user.id" )
    private Long userId;

    @JsonKey( key = FavouriteKeys.DASHBOARD_ID )
    @Column( name = CRMFAVOURITEVIEW.CRM_DASHBOARD_ID )
    @NotNull( groups = { Group.Create.class, Group.Update.class, Group.Delete.class },
              message = "i18n.analytics.favourite.invalid.dashboard.id" )
    private Long dashboardId;
}
