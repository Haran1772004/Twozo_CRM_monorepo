package com.tlc.analytics.config.model.settings;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.config.model.settings.font.FontSettings;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the title settings details
 * </p>
 *
 * @author Jamesh
 * @version 1.0
 */
@SuperBuilder
public class TitleSettings extends BaseModel
{
    private String title;

    private FontSettings fontSettings;
}
