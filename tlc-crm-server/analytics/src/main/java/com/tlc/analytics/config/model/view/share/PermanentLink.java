package com.tlc.analytics.config.model.view.share;

import com.tlc.analytics.sql.resource.ANAPERMANENTLINK;
import com.tlc.analytics.config.keys.view.share.PermanentLinkKeys;

import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;
import com.tlc.analytics.config.model.BaseModel;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.experimental.SuperBuilder;


/**
 * <p>
 * A model that manages the view permanent link details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 * @see BaseModel
 */
@SuperBuilder
@Getter
@Table( name = ANAPERMANENTLINK.TABLE )
public class PermanentLink extends BaseModel
{
    @JsonKey( key = PermanentLinkKeys.VIEW_ID )
    @Column( name = ANAPERMANENTLINK.VIEW_ID )
    @NotNull( groups = { Group.Create.class, Group.Update.class, Group.Delete.class },
              message = "i18n.analytics.permanent.link.invalid.view.id" )
    private Long viewId;

    @JsonKey( key = PermanentLinkKeys.LINK )
    @Column( name = ANAPERMANENTLINK.ACCESS_LINK )
    @NotBlank( groups = { Group.Create.class },
               message = "i18n.analytics.permanent.link.invalid.access.link" )
    @Size( max = 200 )
    private String link;

    @JsonKey( key = PermanentLinkKeys.PASSWORD )
    @Column( name = ANAPERMANENTLINK.PASSWORD )
    @NotEmpty( groups = { Group.Create.class, Group.Update.class },
               message = "i18n.analytics.permanent.link.invalid.password" )
    @Size( max = 200 )
    private String password;

    @JsonKey( key = PermanentLinkKeys.EXPIRY_TIME )
    @Column( name = ANAPERMANENTLINK.EXPIRY_TIME )
    @NotEmpty( groups = { Group.Create.class, Group.Update.class },
               message = "i18n.analytics.permanent.link.invalid.expiry.time" )
    private Long expiryTime;

    @JsonKey( key = PermanentLinkKeys.CREATOR )
    @Column( name = ANAPERMANENTLINK.CREATED_BY )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.permanent.link.invalid.created.by" )
    private Long createdBy;

    @JsonKey( key = PermanentLinkKeys.CREATED_AT )
    @Column( name = ANAPERMANENTLINK.CREATED_TIME )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.permanent.link.invalid.created.time" )
    private Long createdTime;

    @JsonKey( key = PermanentLinkKeys.EDITOR )
    @Column( name = ANAPERMANENTLINK.MODIFIED_BY )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.permanent.link.invalid.modified.by" )
    private Long modifiedBy;

    @JsonKey( key = PermanentLinkKeys.EDITED_AT )
    @Column( name = ANAPERMANENTLINK.MODIFIED_TIME )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.permanent.link.invalid.modified.time" )
    private Long modifiedTime;
}
