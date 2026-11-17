package com.tlc.analytics.config.model.view.report.chart.legenderSettings;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.sql.resource.ANALEGENDERPOINTS;
import com.tlc.analytics.config.keys.view.report.chart.legenderSettings.LegenderPointsKeys;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Table;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the legender points details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANALEGENDERPOINTS.TABLE )
public class LegenderPoints extends BaseModel
{
    @JsonKey( key = LegenderPointsKeys.LEGENDER_SETTINGS_ID )
    @Column( name = ANALEGENDERPOINTS.SETTINGS_ID )
    @NotNull( groups = { Group.Create.class, Group.Update.class },
              message = "i18n.analytics.legender.points.invalid.settings.id" )
    private Long legenderSettingsId;

    @JsonKey( key = LegenderPointsKeys.DATA_POINT )
    @Column( name = ANALEGENDERPOINTS.VALUE )
    @NotNull( groups = { Group.Create.class, Group.Update.class },
              message = "i18n.analytics.legender.points.invalid.value" )
    private String value;

    @JsonKey( key  = LegenderPointsKeys.COLOR_ID )
    @Column( name = ANALEGENDERPOINTS.COLOR_ID )
    @NotNull( groups = { Group.Create.class, Group.Update.class },
              message = "i18n.analytics.legender.points.invalid.color.id" )
    private Long colorId;
}
