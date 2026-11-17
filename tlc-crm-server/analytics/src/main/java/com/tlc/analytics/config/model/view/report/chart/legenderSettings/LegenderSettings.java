package com.tlc.analytics.config.model.view.report.chart.legenderSettings;

import com.tlc.analytics.sql.resource.ANALEGENDERPOINTS;
import com.tlc.analytics.sql.resource.ANALEGENDERSETTINGS;
import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.config.keys.view.report.chart.legenderSettings.LegenderSettingKeys;
import com.tlc.analytics.config.model.view.Position;

import com.tlc.entity.annotation.Cascade;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Relation;
import com.tlc.entity.annotation.Table;
import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotNull;
import java.util.Collection;

/**
 * <p>
 * A model that manages the legender setting details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANALEGENDERSETTINGS.TABLE )
public class LegenderSettings extends BaseModel
{

    @JsonKey( key = LegenderSettingKeys.POSITION  )
    @Column( name = ANALEGENDERSETTINGS.POSITION_ID )
    @NotNull( groups = { Group.Create.class, Group.Update.class },
              message = "i18n.analytics.legender.settings.invalid.position.id" )
    private Integer positionId;

    @JsonKey( key = LegenderSettingKeys.LEGENDER_TITLE )
    @Column( name = ANALEGENDERSETTINGS.LEGENDER_NAME )
    @NotNull( groups = { Group.Create.class, Group.Update.class },
              message = "i18n.analytics.legender.settings.invalid.title" )
    private String legenderTitle;

    @JsonKey( key= LegenderSettingKeys.LEGENDER_POINTS )
    @Join( relation = Relation.ONE_TO_MANY,
           cascades = Cascade.ALL,
           joinTables = @JoinTable( joinColumns = @JoinColumn( referenceColumn = ANALEGENDERPOINTS.SETTINGS_ID ),
                                    entityClass = LegenderPoints.class ) )
    @Valid
    private Collection<LegenderPoints> points;

    public Position getLegenderPosition()
    {
        return Position.getPosition( positionId );
    }
}
