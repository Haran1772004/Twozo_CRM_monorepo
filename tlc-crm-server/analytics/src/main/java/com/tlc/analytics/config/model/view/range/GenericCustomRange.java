package com.tlc.analytics.config.model.view.range;

import com.tlc.analytics.config.keys.view.range.CustomRangeKeys;

import com.tlc.entity.annotation.JsonKey;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the generic custom range details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 * @see CustomRange
 */
@SuperBuilder
@Getter
public class GenericCustomRange<T> extends CustomRange<T>
{
    @JsonKey( key = CustomRangeKeys.FROM )
    @NotNull( groups = { Group.Create.class, Group.Update.class },
              message = "i18n.analytics.generic.custom.range.invalid.from.value" )
    private T fromValue;

    @JsonKey( key = CustomRangeKeys.TO )
    @NotNull( groups = { Group.Create.class, Group.Update.class },
              message = "i18n.analytics.generic.custom.range.invalid.to.value" )
    private T toValue;

    /**
     * Retrieves the from value of the custom range.
     *
     * @return The from value of type T.
     */
    @Override
    public T getFromValue()
    {
        return fromValue;
    }

    /**
     * Retrieves the to value of the custom range.
     *
     * @return The to value of type T.
     */
    @Override
    public T getTOValue()
    {
        return toValue;
    }
}
