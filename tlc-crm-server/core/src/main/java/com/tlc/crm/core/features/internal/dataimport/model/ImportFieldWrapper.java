package com.tlc.crm.core.features.internal.dataimport.model;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.l10n.temporal.model.TimezoneInfo;

/**
 * A model which holds the data needed to format the import value
 *
 * @param fieldInfo    Represents the {@link FieldInfo}
 * @param timezoneInfo Represents the timezone info of the user
 * @author Maharaja S
 * @version 1.0
 */
public record ImportFieldWrapper(FieldInfo fieldInfo, TimezoneInfo timezoneInfo) {

    public ImportFieldWrapper(final FieldInfo fieldInfo) {
        this(fieldInfo, null);
    }
}