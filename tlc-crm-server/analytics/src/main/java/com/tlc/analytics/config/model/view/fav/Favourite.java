package com.tlc.analytics.config.model.view.fav;

import com.tlc.analytics.config.model.UserIdBasedModel;
import com.tlc.analytics.sql.resource.ANAFAVOURITEVIEW;
import com.tlc.analytics.config.keys.view.favourite.FavouriteKeys;

import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Column;
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
 * @author Aswath
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANAFAVOURITEVIEW.TABLE )
public class Favourite extends UserIdBasedModel
{
    @JsonKey( key = FavouriteKeys.VIEW_ID )
    @Column( name = ANAFAVOURITEVIEW.VIEW_ID )
    @NotNull( groups = { Group.Create.class, Group.Update.class, Group.Delete.class },
              message = "i18n.analytics.favourite.invalid.view.id" )
    private Long viewId;

    /**
     * {@inheritDoc}
     *
     * @return True if the item is a favorite, otherwise false.
     */
    public boolean isFavourite()
    {
        return true;
    }
}
