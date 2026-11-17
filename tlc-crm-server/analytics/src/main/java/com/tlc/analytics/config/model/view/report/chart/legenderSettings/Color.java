package com.tlc.analytics.config.model.view.report.chart.legenderSettings;

import com.tlc.analytics.config.keys.view.report.chart.legenderSettings.ColorKeys;
import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.sql.resource.ANACOLOR;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Table;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;


/**
 * <p>
 * A model that manages the color details
 * </p>
 *
 * @author Jamesh
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANACOLOR.TABLE )
public class Color extends BaseModel
{
    @JsonKey( key = ColorKeys.COLOR_HEX )
    @Column( name = ANACOLOR.COLOR_HEX )
    @NotNull( groups = { Group.Create.class, Group.Update.class },
              message = "i18n.analytics.color.invalid.hex" )
    private String colorHexCode;

    @JsonKey( key = ColorKeys.DISPLAY_NAME )
    @Column( name = ANACOLOR.DISPLAY_NAME )
    @NotNull( groups = { Group.Create.class, Group.Update.class },
              message = "i18n.analytics.color.invalid.display.name" )
    private String displayName;
}
