package com.tlc.analytics.config.model.settings.color;

import com.tlc.analytics.config.internal.ConfigErrorCodes;
import com.tlc.commons.code.ErrorCode;
import lombok.Getter;

/**
 * <p>
 * Represents the color palette types
 * </p>
 *
 * @author James
 * @version 1.0
 */
@Getter
public enum ColorPaletteType
{
    SOLID_COLOR(1,"i18n.analytics.color.palette.solid.color"),
    MONOCHROME_COLOR(2,"i18n.analytics.color.palette.monochrome.color"),
    GRADIENT_COLOR(3,"i18n.analytics.color.palette.gradient.color"),
    PATTERN_COLOR(4,"i18n.analytics.color.palette.pattern.color");

    private final long typeCode;
    private final String i18nKeyOfDisplayName;

    ColorPaletteType( final long typeCode, final String i18nKeyOfDisplayName )
    {
        this.typeCode = typeCode;
        this.i18nKeyOfDisplayName = i18nKeyOfDisplayName;
    }

    public static ColorPaletteType getType( final long typeCode )
    {
        return switch ( (int) typeCode )
                {
                    case 1 -> SOLID_COLOR;
                    case 2 -> MONOCHROME_COLOR;
                    case 3 -> GRADIENT_COLOR;
                    case 4 -> PATTERN_COLOR;
                    default -> throw ErrorCode.get( ConfigErrorCodes.INVALID_COLOR_PALETTE_TYPE,
                                            "i18n.analytics.invalid.color.palette.type" );
                };
    }
}
