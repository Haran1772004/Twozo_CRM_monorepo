package com.tlc.analytics.config.model.settings.color;

import com.tlc.analytics.config.model.BaseModel;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Collection;

/**
 * <p>
 * A model that manages the color settings details
 * </p>
 *
 * @author Jamesh
 * @version 1.0
 */
@SuperBuilder
@Getter
public class ColorSettings extends BaseModel
{
    private Long viewId;

    private Long colorPaletteTypeId;

    private Collection<Color> colorPalette;

    public ColorPaletteType getColorPaletteType(){ return ColorPaletteType.getType( colorPaletteTypeId ); }
}
