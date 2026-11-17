package com.tlc.analytics.config.model.settings.color;

import com.tlc.analytics.config.model.BaseModel;
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
public class Color extends BaseModel
{
    private String colorHexCode;

    private String displayName;
}
