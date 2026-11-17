package com.tlc.analytics.config.model.view.report.dynamic.underlying;

import com.tlc.analytics.config.keys.view.ViewModelKeys;
import com.tlc.analytics.config.model.UserIdBasedModel;
import com.tlc.analytics.sql.resource.ANACOLUMNCONFIG;

import com.tlc.entity.annotation.JsonKey;

import com.tlc.entity.annotation.Table;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the analytics column configuration details
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANACOLUMNCONFIG.TABLE )
public class AnalyticsColumnConfig extends UserIdBasedModel
{

    @JsonKey( key = ViewModelKeys.VIEW_ID )
    @NotNull( groups  = { Group.Create.class, Group.Update.class, Group.Delete.class },
              message = "i18n.analytics.underlying.invalid.view.id" )
    private Long viewId;

    private Long columnId;
}
