package com.tlc.analytics.config.model.view.share;

import com.tlc.analytics.sql.resource.ANAVIEWACCESSPERMISSION;
import com.tlc.analytics.config.keys.view.share.ViewPermissionKeys;
import com.tlc.analytics.config.model.UserIdBasedModel;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Table;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.SuperBuilder;


/**
 * <p>
 * A model that manages the view permission details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 * @see UserIdBasedModel
 */
@SuperBuilder
@Getter
@Table( name = ANAVIEWACCESSPERMISSION.TABLE )
public class ViewPermission extends UserIdBasedModel
{
    @JsonKey( key = ViewPermissionKeys.VIEW_ID )
    @Column( name = ANAVIEWACCESSPERMISSION.VIEW_ID )
    @NotNull( groups = { Group.Create.class, Group.Update.class, Group.Delete.class },
              message = "i18n.analytics.view.permission.invalid.view.id" )
    private Long viewId;

    @JsonKey( key = ViewPermissionKeys.SHARED_BY )
    @Column( name = ANAVIEWACCESSPERMISSION.SHARED_BY )
    @NotNull( groups = { Group.Create.class, Group.Update.class, Group.Delete.class },
              message = "i18n.analytics.view.permission.invalid.shared.by" )
    private Long sharedBy;

    @JsonKey( key = ViewPermissionKeys.ACCESS_LEVEL )
    @Getter( AccessLevel.NONE )
    @Column( name = ANAVIEWACCESSPERMISSION.ACCESS_LEVEL )
    @NotNull( groups = { Group.Create.class, Group.Update.class, Group.Delete.class },
              message = "i18n.analytics.view.permission.invalid.access.level" )
    private Integer accessLevel;

    @JsonKey( key = ViewPermissionKeys.SHARED_TIME )
    @Column( name = ANAVIEWACCESSPERMISSION.SHARED_TIME )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.view.permission.invalid.shared.time" )
    private Long sharedTime;

    @JsonKey( key = ViewPermissionKeys.MODIFIED_BY )
    @Column( name = ANAVIEWACCESSPERMISSION.MODIFIED_BY )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.view.permission.invalid.modified.by" )
    private Long modifiedBy;

    @JsonKey( key = ViewPermissionKeys.MODIFIED_TIME )
    @Column( name = ANAVIEWACCESSPERMISSION.MODIFIED_TIME )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.view.permission.invalid.modified.time" )
    private Long modifiedTime;

    public PermissionType getPermissionType()
    {
        return PermissionType.getType( accessLevel );
    }
}
