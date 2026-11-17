package com.tlc.analytics.config.model;

import com.tlc.analytics.config.keys.CommonBaseModelKeys;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the base details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 * @see BaseModel
 */
@SuperBuilder
@Getter
public class CommonBaseModel extends BaseModel
{
    @JsonKey( key = CommonBaseModelKeys.DISPLAY_NAME)
    @Column( name = CommonBaseModelKeys.DISPLAY_NAME_COLUMN)
    @NotBlank( groups = { Group.Create.class },
               message = "i18n.analytics.common.base.model.invalid.display.name" )
    @Size( max = 20 )
    private String displayName;

    @JsonKey( key = CommonBaseModelKeys.MODIFIED_TIME)
    @Column( name = CommonBaseModelKeys.MODIFIED_TIME_COLUMN)
    @NotNull( groups = { Group.Create.class, Group.Update.class },
              message = "i18n.analytics.common.base.model.invalid.modified.time" )
    private Long modifiedTime;

    @JsonKey( key = CommonBaseModelKeys.IS_DELETED)
    @Column( name = CommonBaseModelKeys.IS_DELETED_COLUMN)
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.common.base.model.invalid.deleted" )
    private Boolean isDeleted;
}
