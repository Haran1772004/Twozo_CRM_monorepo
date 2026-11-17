package com.tlc.analytics.config.model;

import com.tlc.analytics.config.keys.UserKeys;

import com.tlc.validator.type.Group;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Column;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class UserIdBasedModel extends BaseModel
{
    @JsonKey( key = UserKeys.USER_ID )
    @Column( name = UserKeys.USER_ID_COLUMN )
    @NotNull( groups = { Group.Create.class, Group.Update.class, Group.Delete.class },
              message = "i18n.analytics.user.id.base.model.invalid.user.id" )
    private Long userId;
}
