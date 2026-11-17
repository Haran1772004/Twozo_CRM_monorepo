package com.tlc.analytics.config.model.settings.font;

import com.tlc.analytics.config.model.BaseModel;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the font setting details
 * </p>
 *
 * @author Jamesh
 * @version 1.0
 */
@SuperBuilder
@Getter
public class FontSettings extends BaseModel
{

    private Long fontTypeId;

    private String fontFormat;

    private Long fontColorId;

    private Long fontSize;

    private String fontDirection;
}
